package com.example.rutalogcliente.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cola local de escrituras por enviar a la API (RF12). Se conserva aunque se cierre la app.
 * Cada operación tiene un UUID propio: si se reenvía, el servidor la reconoce y no la duplica (RF15).
 */
@Entity(tableName = "operaciones_pendientes")
data class OperacionPendienteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuidOperacion: String,
    /** Tabla afectada; en esta app siempre "envios". */
    val entidad: String,
    val idEntidadLocal: Int,
    /** CREAR, ACTUALIZAR o ELIMINAR. */
    val tipoOperacion: String,
    /** Datos de la operación en JSON. */
    val payload: String,
    val fechaRegistro: Long,
    /** Nombre de EstadoSincronizacion. */
    val estado: String,
    val intentos: Int = 0,
    val mensajeError: String? = null
)

object TipoOperacion {
    const val CREAR = "CREAR"
    const val ACTUALIZAR = "ACTUALIZAR"
    const val ELIMINAR = "ELIMINAR"
}
