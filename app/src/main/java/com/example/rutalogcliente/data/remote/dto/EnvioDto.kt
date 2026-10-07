package com.example.rutalogcliente.data.remote.dto

/** Formato JSON de la API REST. Los nombres coinciden con los que envía y recibe el servidor. */

data class EnvioDto(
    val id: Int,
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val costoEnvio: Double,
    val estado: String,
    val transportistaAsignado: String?,
    val version: Int
)

data class EnviosResponse(
    val datos: List<EnvioDto>,
    val servidorAhora: String?
)

data class CrearEnvioRequest(
    val uuidOperacion: String,
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double
)

data class ActualizarEnvioRequest(
    val uuidOperacion: String,
    val ruta: String,
    val pesoKg: Double,
    val version: Int
)

data class LoginRequest(
    val correo: String,
    val clave: String
)

data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)

data class LoginResponse(
    val token: String,
    val usuario: UsuarioDto
)

/** Cuerpo de las respuestas de error. En un conflicto (409) incluye el envío real del servidor. */
data class ErrorDto(
    val codigo: String?,
    val mensaje: String?,
    val envio: EnvioDto?
)
