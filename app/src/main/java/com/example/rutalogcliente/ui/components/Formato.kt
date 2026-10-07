package com.example.rutalogcliente.ui.components

import java.util.Locale

private val localePeru: Locale = Locale.forLanguageTag("es-PE")

fun formatoSoles(monto: Double): String = String.format(localePeru, "S/ %,.2f", monto)

fun formatoPeso(kg: Double): String = String.format(localePeru, "%,.2f kg", kg)

private val formatoFecha = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", localePeru)

/** Convierte una fecha en milisegundos a "dd/MM/yyyy HH:mm:ss" en la hora del teléfono. */
fun formatoFechaHora(milisegundos: Long): String =
    java.time.Instant.ofEpochMilli(milisegundos)
        .atZone(java.time.ZoneId.systemDefault())
        .format(formatoFecha)
