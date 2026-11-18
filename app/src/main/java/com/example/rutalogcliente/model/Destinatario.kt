package com.example.rutalogcliente.model

/** Persona que recibe el envío. El courier solo ve estos datos durante la entrega (ética del caso). */
data class Destinatario(
    val nombre: String,
    val telefono: String?,
    val direccion: String
)

/** Lo que el cliente escribe en el formulario: destinatario y, si corresponde, el monto a cobrar. */
data class DestinatarioForm(
    val nombre: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val cobrarContraentrega: Boolean = false,
    val monto: String = ""
) {
    fun validar(): String? =
        ReglasEnvio.validarDestinatario(nombre, telefono, direccion, cobrarContraentrega, monto)

    fun aDestinatario(): Destinatario =
        Destinatario(nombre = nombre.trim(), telefono = telefono.trim().ifEmpty { null }, direccion = direccion.trim())

    /** 0 si el envío es prepagado. */
    fun montoContraentrega(): Double =
        if (cobrarContraentrega) ReglasEnvio.leerMonto(monto)?.let { Math.round(it * 100) / 100.0 } ?: 0.0 else 0.0

    companion object {
        /** Carga en el formulario los datos guardados de un envío (al editar). */
        fun desde(envio: Envio): DestinatarioForm = DestinatarioForm(
            nombre = envio.destinatario?.nombre.orEmpty(),
            telefono = envio.destinatario?.telefono.orEmpty(),
            direccion = envio.destinatario?.direccion.orEmpty(),
            cobrarContraentrega = envio.cobraContraentrega,
            monto = if (envio.cobraContraentrega) envio.montoContraentrega.toString() else ""
        )
    }
}

/**
 * HU-R01: reglas del destinatario y del cobro contraentrega. Son las mismas que valida la API,
 * así un envío registrado sin conexión no se rechaza al sincronizar.
 */
object ReglasEnvio {

    const val MONTO_MAXIMO = 10_000.0
    private val CELULAR = Regex("^9\\d{8}$")

    fun leerMonto(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

    /** Devuelve el mensaje de error, o null si los datos son válidos. */
    fun validarDestinatario(
        nombre: String,
        telefono: String,
        direccion: String,
        cobrarContraentrega: Boolean,
        montoTexto: String
    ): String? {
        val monto = leerMonto(montoTexto)
        return when {
            nombre.isBlank() -> "Indica el nombre de quien recibe."
            nombre.trim().length > 80 -> "El nombre admite hasta 80 caracteres."
            telefono.isBlank() -> "Indica el celular del destinatario: el courier lo llama antes de llegar."
            !CELULAR.matches(telefono.trim()) -> "El celular debe tener 9 dígitos y empezar con 9."
            direccion.isBlank() -> "Indica la dirección de entrega."
            direccion.trim().length > 160 -> "La dirección admite hasta 160 caracteres."
            !cobrarContraentrega -> null
            montoTexto.isBlank() -> "Ingresa el monto que se cobrará al destinatario."
            monto == null -> "El monto debe ser un número, por ejemplo 189.90."
            monto <= 0.0 -> "El monto a cobrar debe ser mayor que 0."
            monto > MONTO_MAXIMO -> "El monto contraentrega máximo es S/ 10 000."
            else -> null
        }
    }
}
