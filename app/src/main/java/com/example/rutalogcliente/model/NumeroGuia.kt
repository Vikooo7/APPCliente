package com.example.rutalogcliente.model

import java.time.LocalDate

/**
 * RF07: número de guía con el formato RLP-AA-NNNNNN-D.
 * AA = año, NNNNNN = correlativo, D = dígito verificador (suma de dígitos mod 10).
 * Ejemplo: el correlativo 100012 en 2026 da la guía RLP-26-100012-4.
 *
 * La app genera la guía al insertar el envío, aunque no haya conexión. Si al sincronizar
 * el servidor ya tiene esa guía, asigna otra (correlativo desde 500000) y la app la actualiza.
 */
object NumeroGuia {

    const val PRIMER_CORRELATIVO = 100_001
    const val INICIO_SERVIDOR = 500_000

    fun generar(correlativo: Int, anio: Int = LocalDate.now().year): String {
        val base = correlativo.toString()
        val verificador = base.sumOf { it.digitToInt() } % 10
        return "RLP-%02d-%s-%d".format(anio % 100, base, verificador)
    }

    /** Extrae el correlativo de una guía; null si no tiene el formato esperado. */
    fun correlativo(guia: String): Int? = guia.split('-').getOrNull(2)?.toIntOrNull()

    /** Quita guiones y espacios para comparar lo que escribe el usuario con lo guardado. */
    fun normalizar(texto: String): String = texto.uppercase().filter { it.isLetterOrDigit() }
}
