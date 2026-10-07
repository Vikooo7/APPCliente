package com.example.rutalogcliente.model

/**
 * Estado de sincronización de un registro o de una operación con la API.
 * Es independiente del estado de negocio del envío (pendiente, recogido, en tránsito…).
 */
enum class EstadoSincronizacion(val etiqueta: String) {
    PENDIENTE("Pendiente de enviar"),
    ENVIANDO("Enviando"),
    SINCRONIZADO("Sincronizado"),
    ERROR("Rechazado");

    companion object {
        fun desde(nombre: String): EstadoSincronizacion = entries.firstOrNull { it.name == nombre } ?: PENDIENTE
    }
}
