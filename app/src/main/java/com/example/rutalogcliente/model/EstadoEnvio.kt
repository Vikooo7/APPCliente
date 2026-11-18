package com.example.rutalogcliente.model

/**
 * Estados de negocio de un envío. En Room y en la API se guarda el [codigo].
 * La App Cliente registra los envíos como PENDIENTE (el REGISTRADO de la ficha del proyecto
 * final); la operación los pasa a los demás. FALLIDO lo registra el courier, REPROGRAMADO el
 * operador y DEVUELTO llega al tercer intento fallido (RN-05).
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
    ENTREGADO("entregado", "Entregado", 4, "Entregado y firmado por el destinatario."),
    FALLIDO("fallido", "Entrega fallida", 3, "El courier no pudo entregar. RutaLog reprogramará la entrega."),
    REPROGRAMADO("reprogramado", "Reprogramado", 2, "La entrega se reprogramó y saldrá de nuevo a reparto."),
    DEVUELTO("devuelto", "Devuelto", 4, "Tres intentos fallidos: la carga vuelve a tu dirección.");

    companion object {
        /** RN-05: al tercer intento fallido el envío vuelve al remitente. */
        const val MAX_INTENTOS = 3

        fun desde(codigo: String): EstadoEnvio = entries.firstOrNull { it.codigo == codigo } ?: PENDIENTE
    }
}

val Envio.estadoEnvio: EstadoEnvio
    get() = EstadoEnvio.desde(estado)

/** El cliente solo puede editar o eliminar un envío mientras no lo hayan recogido. */
val Envio.esModificable: Boolean
    get() = estadoEnvio == EstadoEnvio.PENDIENTE
