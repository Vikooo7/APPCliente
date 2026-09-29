package com.example.rutalogcliente.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla "envios". Tiene el mismo nombre y los mismos campos en la App Cliente
 * y en la App Operador.
 *
 * - ruta: nombre de la ruta, por ejemplo "Lima → Arequipa" (ver CatalogoRutas).
 * - estado: código de EstadoEnvio ("pendiente", "recogido", "en_transito",
 *   "en_reparto" o "entregado").
 * - transportistaAsignado: null mientras el operador no asigne a nadie.
 */
@Entity(tableName = "envios")
data class Envio(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val costoEnvio: Double,
    val estado: String,
    val transportistaAsignado: String?
)
