package com.example.rutalogcliente.model

/**
 * Envío tal como lo usan las pantallas y los ViewModel.
 * La tabla de Room es EnvioEntity y el formato de la API es EnvioDto (ver data/mapper).
 */
data class Envio(
    /** Identificador local (Room). */
    val id: Int,
    /** Identificador en el servidor; null mientras no se haya sincronizado. */
    val idRemoto: Int?,
    /** Identificador único generado en el teléfono; evita duplicados al sincronizar. */
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val costoEnvio: Double,
    val estado: String,
    val transportistaAsignado: String?,
    /** Versión del servidor; sirve para detectar conflictos. */
    val version: Int,
    val estadoSync: EstadoSincronizacion,
    /** Motivo del rechazo del servidor, si lo hubo. */
    val mensajeError: String?,
    /** Quién recibe el envío (proyecto final). Los envíos del parcial no lo tienen. */
    val destinatario: Destinatario? = null,
    /** Monto que el courier cobra al destinatario; 0 si el envío es prepagado (RN-01). */
    val montoContraentrega: Double = 0.0,
    /** Intentos de entrega fallidos (RN-05: al tercero, el envío vuelve al remitente). */
    val intentos: Int = 0,
    /** RF-R02: el costo del envío se paga con QR de Banco Andino antes del recojo. */
    val pagoEstado: String = "NO_REQUERIDO",
    /** Guía que generó el celular, si el servidor asignó otra al sincronizar porque ya existía. */
    val guiaAnterior: String? = null
) {
    val cobraContraentrega: Boolean get() = montoContraentrega > 0.0
    val porPagar: Boolean get() = pagoEstado == "POR_PAGAR"
    val pagado: Boolean get() = pagoEstado == "PAGADO"
}
