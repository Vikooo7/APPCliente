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
    val mensajeError: String?
)
