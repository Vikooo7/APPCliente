package com.example.rutalogcliente.data.remote

import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.remote.dto.EnvioDto
import com.example.rutalogcliente.data.remote.dto.ErrorDto
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitClient {

    /** Dirección pública de la API en Vercel. */
    const val URL_BASE = "https://app-api-rutalog.vercel.app/"

    /**
     * @param token devuelve el token de sesión actual, o null si no hay sesión.
     * @param simularError true para pedirle a la API un error 503 (prueba de error del servidor).
     */
    fun crear(token: () -> String?, simularError: () -> Boolean): ApiService {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { cadena ->
                val peticion = cadena.request().newBuilder().apply {
                    token()?.let { header("Authorization", "Bearer $it") }
                    if (simularError()) header("X-Simular-Error", "1")
                }.build()
                cadena.proceed(peticion)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

/** Resultado de una llamada a la API, ya clasificado para que el Repository decida qué hacer. */
sealed interface ResultadoApi<out T> {

    data class Exito<T>(val valor: T) : ResultadoApi<T>

    /** El servidor respondió, pero rechazó la operación (validación, permiso o conflicto). */
    data class Rechazo(
        val http: Int,
        val codigo: String,
        val mensaje: String,
        val envio: EnvioDto? = null
    ) : ResultadoApi<Nothing>

    /** No hubo respuesta útil: sin conexión, tiempo de espera agotado o error 5xx. Se puede reintentar. */
    data class Fallo(val mensaje: String, val sinConexion: Boolean) : ResultadoApi<Nothing>
}

private val gson = Gson()

/** Ejecuta una llamada de Retrofit y convierte errores HTTP y de red en un [ResultadoApi]. */
suspend fun <T> llamarApi(bloque: suspend () -> Response<T>): ResultadoApi<T> = try {
    val respuesta = bloque()
    val cuerpo = respuesta.body()
    when {
        respuesta.isSuccessful && cuerpo != null -> ResultadoApi.Exito(cuerpo)
        respuesta.isSuccessful -> ResultadoApi.Fallo("El servidor envió una respuesta vacía.", sinConexion = false)
        else -> {
            val error = leerError(respuesta)
            if (respuesta.code() >= 500) {
                ResultadoApi.Fallo(
                    error?.mensaje ?: "El servidor no está disponible (${respuesta.code()}).",
                    sinConexion = false
                )
            } else {
                ResultadoApi.Rechazo(
                    http = respuesta.code(),
                    codigo = error?.codigo ?: "ERROR_${respuesta.code()}",
                    mensaje = error?.mensaje ?: "El servidor rechazó la operación (${respuesta.code()}).",
                    envio = error?.envio
                )
            }
        }
    }
} catch (e: CancellationException) {
    throw e
} catch (e: IOException) {
    ResultadoApi.Fallo("No se pudo conectar con el servidor.", sinConexion = true)
} catch (e: Exception) {
    ResultadoApi.Fallo("Error inesperado: ${e.message}", sinConexion = false)
}

private fun leerError(respuesta: Response<*>): ErrorDto? = try {
    respuesta.errorBody()?.string()?.let { gson.fromJson(it, ErrorDto::class.java) }
} catch (e: Exception) {
    null
}
