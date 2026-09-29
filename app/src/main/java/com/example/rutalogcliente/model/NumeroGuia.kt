package com.example.rutalogcliente.model

import java.time.LocalDate

/**
 * RF07: número de guía simulado con el formato RLP-AA-NNNNNN-D.
 * AA = año, NNNNNN = 100000 + id del envío, D = dígito verificador (suma de dígitos mod 10).
 * Ejemplo: el envío con id 12 en 2026 recibe la guía RLP-26-100012-4.
 */
object NumeroGuia {

    fun generar(id: Int, anio: Int = LocalDate.now().year): String {
        val correlativo = (100_000 + id).toString()
        val verificador = correlativo.sumOf { it.digitToInt() } % 10
        return "RLP-%02d-%s-%d".format(anio % 100, correlativo, verificador)
    }

    /** Quita guiones y espacios para comparar lo que escribe el usuario con lo guardado. */
    fun normalizar(texto: String): String = texto.uppercase().filter { it.isLetterOrDigit() }
}
