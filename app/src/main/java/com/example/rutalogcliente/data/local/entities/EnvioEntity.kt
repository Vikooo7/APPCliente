package com.example.rutalogcliente.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla "envios": los campos del caso (id, numeroGuia, ruta, pesoKg, costoEnvio, estado,
 * transportistaAsignado) más los que exige la sincronización: identificador remoto, UUID,
 * versión y estado de sincronización.
 */
@Entity(tableName = "envios", indices = [Index(value = ["uuid"], unique = true)])
data class EnvioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val idRemoto: Int?,
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double,
    val costoEnvio: Double,
    val estado: String,
    val transportistaAsignado: String?,
    val version: Int = 1,
    /** Nombre de EstadoSincronizacion: PENDIENTE, ENVIANDO, SINCRONIZADO o ERROR. */
    val estadoSync: String,
    /** true cuando el usuario lo eliminó sin conexión y falta confirmarlo en el servidor. */
    val eliminadoLocal: Boolean = false,
    val mensajeError: String? = null
)
