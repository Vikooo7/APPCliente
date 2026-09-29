package com.example.rutalogcliente.model

import com.example.rutalogcliente.data.local.Envio

/**
 * Estados de un envío. En Room se guarda el [codigo]. La App Cliente registra
 * los envíos como PENDIENTE; la App Operador los pasa a los demás estados.
 */
enum class EstadoEnvio(
    val codigo: String,
    val etiqueta: String,
    val paso: Int,
    val descripcion: String
) {
    PENDIENTE("pendiente", "Pendiente de recojo", 0, "Guía emitida. Esperando que recojan la carga."),
    RECOGIDO("recogido", "Recogido", 1, "La carga fue recogida y verificada en almacén."),
    EN_TRANSITO("en_transito", "En tránsito", 2, "La unidad va en camino a la ciudad de destino."),
    EN_REPARTO("en_reparto", "En reparto", 3, "La carga salió a reparto con el destinatario."),
    ENTREGADO("entregado", "Entregado", 4, "Entregado y firmado por el destinatario.");

    companion object {
        fun desde(codigo: String): EstadoEnvio = entries.firstOrNull { it.codigo == codigo } ?: PENDIENTE
    }
}

val Envio.estadoEnvio: EstadoEnvio
    get() = EstadoEnvio.desde(estado)

/** El cliente solo puede editar o eliminar un envío mientras no lo hayan recogido. */
val Envio.esModificable: Boolean
    get() = estadoEnvio == EstadoEnvio.PENDIENTE
