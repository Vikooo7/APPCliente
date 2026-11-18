package com.example.rutalogcliente.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tabla "envios": los campos del caso (id, numeroGuia, ruta, pesoKg, costoEnvio, estado,
 * transportistaAsignado) más los que exige la sincronización: identificador remoto, UUID,
 * versión y estado de sincronización. Desde la versión 3, también el destinatario, el monto
 * contraentrega y los intentos fallidos (proyecto final).
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
    val mensajeError: String? = null,
    val destinatarioNombre: String? = null,
    val destinatarioTelefono: String? = null,
    val destinatarioDireccion: String? = null,
    @ColumnInfo(defaultValue = "0") val montoContraentrega: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val intentos: Int = 0,
    /** RF-R02: NO_REQUERIDO (envíos anteriores), POR_PAGAR o PAGADO (confirmado por Banco Andino). */
    @ColumnInfo(defaultValue = "NO_REQUERIDO") val pagoEstado: String = "NO_REQUERIDO"
)
