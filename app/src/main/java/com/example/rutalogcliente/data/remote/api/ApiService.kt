package com.example.rutalogcliente.data.remote.api

import com.example.rutalogcliente.data.remote.dto.ActualizarEnvioRequest
import com.example.rutalogcliente.data.remote.dto.CobroDto
import com.example.rutalogcliente.data.remote.dto.CobrosResponse
import com.example.rutalogcliente.data.remote.dto.CuentaBancoRequest
import com.example.rutalogcliente.data.remote.dto.CuentaBancoResponse
import com.example.rutalogcliente.data.remote.dto.LiquidacionesResponse
import com.example.rutalogcliente.data.remote.dto.PagoQrRequest
import com.example.rutalogcliente.data.remote.dto.PerfilDto
import com.example.rutalogcliente.data.remote.dto.CrearEnvioRequest
import com.example.rutalogcliente.data.remote.dto.EnvioDto
import com.example.rutalogcliente.data.remote.dto.EnviosResponse
import com.example.rutalogcliente.data.remote.dto.LoginRequest
import com.example.rutalogcliente.data.remote.dto.LoginResponse
import com.example.rutalogcliente.data.remote.dto.SocialRequest
import com.example.rutalogcliente.data.remote.dto.RegistroRequest
import com.example.rutalogcliente.data.remote.dto.SeguimientoDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/** Endpoints de la API REST de RutaLog (Node.js + Express en Vercel, base de datos en Neon). */
interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): Response<LoginResponse>

    /** La API valida el token de Firebase (Google) y entrega su propia sesión. */
    @POST("api/auth/social")
    suspend fun loginSocial(@Body cuerpo: SocialRequest): Response<LoginResponse>

    /** Crea una cuenta de cliente y devuelve la sesión iniciada (201), o 409 si el correo ya existe. */
    @POST("api/auth/registro")
    suspend fun registrar(@Body cuerpo: RegistroRequest): Response<LoginResponse>

    /** Foto o firma de la prueba de entrega (/api/archivos/{id}); la API comprueba que el envío sea del cliente. */
    @GET
    suspend fun descargarArchivo(@Url url: String): Response<ResponseBody>

    // ---------- Pagos con Banco Andino (la API de RutaLog habla con el banco; la app no) ----------

    /** RF-R02: QR para pagar el costo del envío. Si ya hay uno vigente, la API devuelve el mismo. */
    @POST("api/pagos/qr")
    suspend fun crearPagoQr(@Body cuerpo: PagoQrRequest): Response<CobroDto>

    /** Estado del pago; la API pregunta al banco en ese momento (RNF-05). */
    @GET("api/pagos/{id}")
    suspend fun consultarPago(@Path("id") id: String): Response<CobroDto>

    /** RF-R04: cobros contraentrega con guía y número de operación. */
    @GET("api/cobros")
    suspend fun obtenerCobros(): Response<CobrosResponse>

    /** RF-R05: liquidaciones transferidas a la cuenta del remitente. */
    @GET("api/liquidaciones")
    suspend fun obtenerLiquidaciones(): Response<LiquidacionesResponse>

    @GET("api/usuarios/yo")
    suspend fun obtenerPerfil(): Response<PerfilDto>

    /** Cuenta de Banco Andino donde se recibe la liquidación; la API la valida con el banco. */
    @PUT("api/usuarios/yo/cuenta-banco")
    suspend fun guardarCuentaBanco(@Body cuerpo: CuentaBancoRequest): Response<CuentaBancoResponse>

    /** El servidor devuelve solo los envíos del cliente con sesión iniciada. */
    @GET("api/envios")
    suspend fun obtenerEnvios(): Response<EnviosResponse>

    @GET("api/envios/{id}")
    suspend fun obtenerEnvio(@Path("id") id: Int): Response<EnvioDto>

    /** HU-R02: seguimiento por número de guía (eventos con hora y lugar, y prueba de entrega). */
    @GET("api/envios/{guia}")
    suspend fun obtenerSeguimiento(@Path("guia") guia: String): Response<SeguimientoDto>

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
