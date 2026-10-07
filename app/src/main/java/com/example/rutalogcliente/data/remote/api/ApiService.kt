package com.example.rutalogcliente.data.remote.api

import com.example.rutalogcliente.data.remote.dto.ActualizarEnvioRequest
import com.example.rutalogcliente.data.remote.dto.CrearEnvioRequest
import com.example.rutalogcliente.data.remote.dto.EnvioDto
import com.example.rutalogcliente.data.remote.dto.EnviosResponse
import com.example.rutalogcliente.data.remote.dto.LoginRequest
import com.example.rutalogcliente.data.remote.dto.LoginResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de la API REST de RutaLog (Node.js + Express en Vercel, base de datos en Neon). */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): Response<LoginResponse>

    /** El servidor devuelve solo los envíos del cliente con sesión iniciada. */
    @GET("api/envios")
    suspend fun obtenerEnvios(): Response<EnviosResponse>

    @GET("api/envios/{id}")
    suspend fun obtenerEnvio(@Path("id") id: Int): Response<EnvioDto>

    @POST("api/envios")
    suspend fun crearEnvio(@Body cuerpo: CrearEnvioRequest): Response<EnvioDto>

    @PUT("api/envios/{id}")
    suspend fun actualizarEnvio(@Path("id") id: Int, @Body cuerpo: ActualizarEnvioRequest): Response<EnvioDto>

    @DELETE("api/envios/{id}")
    suspend fun eliminarEnvio(
        @Path("id") id: Int,
        @Query("uuidOperacion") uuidOperacion: String
    ): Response<ResponseBody>
}
