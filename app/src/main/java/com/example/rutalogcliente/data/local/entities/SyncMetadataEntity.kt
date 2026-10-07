package com.example.rutalogcliente.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Datos de la última sincronización exitosa de cada recurso (RF14). */
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val recurso: String,
    /** Fecha y hora del teléfono, en milisegundos. */
    val ultimaSincronizacionExitosa: Long?,
    /** Hora del servidor en la última descarga. */
    val cursorRemoto: String?
)
