package com.example.rutalogcliente.ui.components

import java.util.Locale

private val localePeru: Locale = Locale.forLanguageTag("es-PE")

fun formatoSoles(monto: Double): String = String.format(localePeru, "S/ %,.2f", monto)

fun formatoPeso(kg: Double): String = String.format(localePeru, "%,.2f kg", kg)
