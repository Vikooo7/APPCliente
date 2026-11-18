package com.example.rutalogcliente.model

/** Un cambio de estado con la hora real en que ocurrió y, si el courier lo registró, el lugar (GPS). */
data class EventoSeguimiento(
    val estado: EstadoEnvio,
    /** Milisegundos. */
    val fechaHora: Long?,
    val latitud: Double?,
    val longitud: Double?
) {
    val tieneLugar: Boolean get() = latitud != null && longitud != null
}

data class IntentoEntrega(
    val numero: Int,
    val motivo: String,
    val fecha: Long?
)

/** HU-R02: prueba de entrega. El DNI de quien recibe llega parcial (****1234). */
data class PruebaEntrega(
    val recibeNombre: String,
    val recibeDni: String,
    val fechaHora: Long?,
    val fotoUrl: String?,
    val firmaUrl: String?
)

/** RF-C07: aviso que RutaLog envió al destinatario (SMS simulado) para reprogramar o por devolución. */
data class AvisoDestinatario(
    val mensaje: String,
    /** Celular enmascarado: *****4321. */
    val destino: String,
    val fecha: Long?
)

/** ML-02 ("Fecha estimada para el remitente"): llegada estimada y su margen de error (MAE del modelo). */
data class LlegadaEstimada(
    /** Milisegundos. */
    val fecha: Long,
    val margenHoras: Int
) {
    /** "jue 19/11 · 14:00 (± 4 h)". Si la hora estimada ya pasó, se avisa en vez de mostrar una fecha vencida. */
    fun texto(ahora: Long = System.currentTimeMillis(), zona: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
        if (fecha + margenHoras * 3_600_000L < ahora) return "Con demora: la entrega está tomando más de lo estimado"
        val d = java.time.Instant.ofEpochMilli(fecha).atZone(zona)
        val dia = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale("es", "PE")).removeSuffix(".")
        return "%s %02d/%02d · %02d:00 (± %d h)".format(dia, d.dayOfMonth, d.monthValue, d.hour, margenHoras)
    }
}

/** Seguimiento de una guía, tal como lo devuelve GET /api/envios/{guia}. */
data class Seguimiento(
    val eventos: List<EventoSeguimiento>,
    val intentos: List<IntentoEntrega>,
    val prueba: PruebaEntrega?,
    val avisos: List<AvisoDestinatario> = emptyList(),
    val estimacion: LlegadaEstimada? = null
)

/** Motivos de una entrega fallida (los mismos de la API). */
object MotivoFalla {
    private val etiquetas = mapOf(
        "ausente" to "Destinatario ausente",
        "direccion_errada" to "Dirección errada",
        "rechazado" to "Rechazado por el destinatario",
        "zona_inaccesible" to "Zona inaccesible"
    )

    fun etiqueta(codigo: String): String = etiquetas[codigo] ?: codigo
}
