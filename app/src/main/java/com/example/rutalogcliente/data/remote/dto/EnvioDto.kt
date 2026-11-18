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
    val version: Int,
    // Campos del proyecto final. Son anulables: Gson deja null lo que el servidor no envíe.
    val destinatarioNombre: String? = null,
    val destinatarioTelefono: String? = null,
    val destinatarioDireccion: String? = null,
    val montoContraentrega: Double? = null,
    val intentos: Int? = null,
    /** NO_REQUERIDO, POR_PAGAR o PAGADO (fase de pagos). */
    val pagoEstado: String? = null
)

data class EnviosResponse(
    val datos: List<EnvioDto>,
    val servidorAhora: String?
)

/** Gson omite los campos null: un envío sin destinatario se registra igual que en el parcial. */
data class CrearEnvioRequest(
    val uuidOperacion: String,
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val destinatarioNombre: String? = null,
    val destinatarioTelefono: String? = null,
    val destinatarioDireccion: String? = null,
    val montoContraentrega: Double? = null
)

/** Lo que llegue null no se envía y el servidor conserva el valor que tenía. */
data class ActualizarEnvioRequest(
    val uuidOperacion: String,
    val ruta: String,
    val pesoKg: Double,
    val version: Int,
    val destinatarioNombre: String? = null,
    val destinatarioTelefono: String? = null,
    val destinatarioDireccion: String? = null,
    val montoContraentrega: Double? = null
)

/** GET /api/envios/{guia}: seguimiento con eventos, intentos fallidos y prueba de entrega (HU-R02). */
data class EventoDto(
    val estado: String,
    val fechaHora: String?,
    val latitud: Double?,
    val longitud: Double?
)

data class IntentoDto(
    val numero: Int,
    val motivo: String,
    val fecha: String?
)

data class PruebaEntregaDto(
    val fotoUrl: String?,
    val firmaUrl: String?,
    val recibeNombre: String?,
    val recibeDni: String?,
    val fechaHora: String?
)

/** RF-C07: aviso enviado al destinatario tras una entrega fallida (SMS simulado, celular enmascarado). */
data class AvisoDto(
    val motivo: String?,
    val canal: String?,
    val destino: String?,
    val mensaje: String?,
    val enviadoEn: String?
)

/** ML-02: fecha estimada de entrega que calcula la API con el modelo del notebook. */
data class EstimacionDto(
    val fechaEstimada: String?,
    val horas: Double?,
    val margenHoras: Int?
)

data class SeguimientoDto(
    val envio: EnvioDto,
    val eventos: List<EventoDto>?,
    val intentosFallidos: List<IntentoDto>?,
    val pruebaEntrega: PruebaEntregaDto?,
    val avisos: List<AvisoDto>?,
    /** null cuando el envío ya se entregó o se devolvió. */
    val estimacion: EstimacionDto? = null
)

data class LoginRequest(
    val correo: String,
    val clave: String
)

/** Cuenta nueva: la API siempre la crea con rol cliente. */
data class RegistroRequest(
    val nombre: String,
    val correo: String,
    val clave: String
)

data class UsuarioDto(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String
)

/** Ingreso con Google: ID token de Firebase y la app desde la que se entra. */
data class SocialRequest(
    val idToken: String,
    val app: String
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
