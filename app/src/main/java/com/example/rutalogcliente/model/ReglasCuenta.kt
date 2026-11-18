package com.example.rutalogcliente.model

/** Errores del formulario "Crear cuenta"; cada campo es null si está bien. */
data class ErroresCuenta(
    val nombre: String? = null,
    val correo: String? = null,
    val clave: String? = null,
    val confirmacion: String? = null
) {
    val hayErrores: Boolean get() = listOfNotNull(nombre, correo, clave, confirmacion).isNotEmpty()
}

/** Mismas reglas que la API (POST /api/auth/registro), para avisar antes de enviar. */
object ReglasCuenta {
    const val CLAVE_MINIMA = 8
    private val FORMATO_CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun limpiarNombre(nombre: String): String = nombre.trim().replace(Regex("\\s+"), " ")

    fun limpiarCorreo(correo: String): String = correo.trim().lowercase()

    fun validar(nombre: String, correo: String, clave: String, confirmacion: String): ErroresCuenta {
        val nombreLimpio = limpiarNombre(nombre)
        val correoLimpio = limpiarCorreo(correo)
        return ErroresCuenta(
            nombre = when {
                nombreLimpio.isEmpty() -> "Ingresa tu nombre."
                nombreLimpio.length !in 3..80 -> "El nombre debe tener entre 3 y 80 caracteres."
                else -> null
            },
            correo = when {
                correoLimpio.isEmpty() -> "Ingresa tu correo."
                correoLimpio.length > 120 || !FORMATO_CORREO.matches(correoLimpio) ->
                    "Ingresa un correo válido, por ejemplo nombre@gmail.com."
                else -> null
            },
            clave = when {
                clave.length < CLAVE_MINIMA -> "Usa al menos $CLAVE_MINIMA caracteres."
                clave.length > 72 -> "La contraseña es demasiado larga."
                clave.none { it.isLetter() } || clave.none { it.isDigit() } -> "Combina letras y números."
                else -> null
            },
            confirmacion = if (confirmacion != clave) "Las contraseñas no coinciden." else null
        )
    }
}
