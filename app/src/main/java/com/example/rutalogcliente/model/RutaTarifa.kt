package com.example.rutalogcliente.model

import kotlin.math.roundToLong

/**
 * Ruta que el cliente puede elegir al registrar un envío, con su tarifa por kg.
 * El campo "ruta" de la tabla envios guarda el [nombre].
 */
data class RutaTarifa(
    val nombre: String,
    val region: String,
    val zona: String,
    val distanciaKm: Int,
    val horasEstimadas: Int,
    val tarifaPorKg: Double,
    val modalidad: String = "Terrestre"
)

/** Cobertura nacional: desde Lima hacia las 25 regiones del Perú, más dos rutas interregionales. */
object CatalogoRutas {

    val zonas = listOf("Lima y Callao", "Norte", "Centro", "Sur", "Oriente")

    val rutas = listOf(
        // Lima y Callao
        RutaTarifa("Lima → Lima (urbano)", "Lima", "Lima y Callao", 45, 3, 0.80),
        RutaTarifa("Lima → Callao", "Callao", "Lima y Callao", 20, 2, 0.70),
        // Norte
        RutaTarifa("Lima → Tumbes", "Tumbes", "Norte", 1270, 20, 3.40),
        RutaTarifa("Lima → Piura", "Piura", "Norte", 980, 15, 2.80),
        RutaTarifa("Lima → Chiclayo", "Lambayeque", "Norte", 770, 12, 2.40),
        RutaTarifa("Lima → Trujillo", "La Libertad", "Norte", 560, 9, 1.90),
        RutaTarifa("Lima → Cajamarca", "Cajamarca", "Norte", 860, 14, 2.60),
        RutaTarifa("Lima → Huaraz", "Áncash", "Norte", 405, 8, 1.70),
        // Centro
        RutaTarifa("Lima → Huánuco", "Huánuco", "Centro", 410, 8, 1.70),
        RutaTarifa("Lima → Cerro de Pasco", "Pasco", "Centro", 300, 7, 1.50),
        RutaTarifa("Lima → Huancayo", "Junín", "Centro", 300, 7, 1.50),
        RutaTarifa("Lima → Huancavelica", "Huancavelica", "Centro", 450, 10, 1.80),
        RutaTarifa("Lima → Ayacucho", "Ayacucho", "Centro", 560, 9, 1.90),
        // Sur
        RutaTarifa("Lima → Ica", "Ica", "Sur", 305, 4, 1.40),
        RutaTarifa("Lima → Abancay", "Apurímac", "Sur", 900, 16, 2.70),
        RutaTarifa("Lima → Cusco", "Cusco", "Sur", 1100, 22, 3.10),
        RutaTarifa("Lima → Arequipa", "Arequipa", "Sur", 1010, 16, 2.90),
        RutaTarifa("Lima → Puno", "Puno", "Sur", 1300, 20, 3.40),
        RutaTarifa("Lima → Moquegua", "Moquegua", "Sur", 1140, 17, 3.10),
        RutaTarifa("Lima → Tacna", "Tacna", "Sur", 1290, 19, 3.30),
        // Oriente
        RutaTarifa("Lima → Chachapoyas", "Amazonas", "Oriente", 1200, 22, 3.30),
        RutaTarifa("Lima → Tarapoto", "San Martín", "Oriente", 1390, 24, 3.60),
        RutaTarifa("Lima → Iquitos", "Loreto", "Oriente", 1800, 96, 4.80, "Terrestre + fluvial (vía Pucallpa)"),
        RutaTarifa("Lima → Pucallpa", "Ucayali", "Oriente", 780, 18, 2.50),
        RutaTarifa("Lima → Puerto Maldonado", "Madre de Dios", "Oriente", 1630, 28, 4.20),
        // Interregionales
        RutaTarifa("Arequipa → Puno", "Puno", "Sur", 290, 6, 1.40),
        RutaTarifa("Cusco → Puerto Maldonado", "Madre de Dios", "Oriente", 480, 10, 1.90)
    )

    fun porNombre(nombre: String): RutaTarifa? = rutas.firstOrNull { it.nombre == nombre }

    /** RF08: costo = pesoKg × tarifaPorKg de la ruta, redondeado a céntimos. */
    fun calcularCosto(pesoKg: Double, tarifaPorKg: Double): Double =
        (pesoKg * tarifaPorKg * 100).roundToLong() / 100.0
}
