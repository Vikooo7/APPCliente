package com.example.rutalogcliente.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.rutalogcliente.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcliente.data.local.entities.SyncMetadataEntity
import kotlinx.coroutines.flow.Flow

/** Operación pendiente junto con la guía del envío al que pertenece (para mostrarla). */
data class OperacionConGuia(
    @Embedded val operacion: OperacionPendienteEntity,
    val numeroGuia: String?
)

/** Cola local de operaciones y datos de la última sincronización. */
@Dao
interface OperacionPendienteDao {

    @Insert
    suspend fun insertar(operacion: OperacionPendienteEntity): Long

    @Update
    suspend fun actualizar(operacion: OperacionPendienteEntity)

    @Delete
    suspend fun eliminar(operacion: OperacionPendienteEntity)

    @Query(
        "SELECT o.*, e.numeroGuia AS numeroGuia FROM operaciones_pendientes o " +
            "LEFT JOIN envios e ON e.id = o.idEntidadLocal ORDER BY o.id"
    )
    fun observarConGuia(): Flow<List<OperacionConGuia>>

    @Query("SELECT * FROM operaciones_pendientes ORDER BY id")
    suspend fun todas(): List<OperacionPendienteEntity>

    /** En orden de registro. Las rechazadas (ERROR) esperan a que el usuario reintente o descarte. */
    @Query("SELECT * FROM operaciones_pendientes WHERE estado != 'ERROR' ORDER BY id")
    suspend fun porEnviar(): List<OperacionPendienteEntity>

    @Query("SELECT * FROM operaciones_pendientes WHERE idEntidadLocal = :idEntidadLocal ORDER BY id")
    suspend fun deEntidad(idEntidadLocal: Int): List<OperacionPendienteEntity>

    @Query("SELECT * FROM operaciones_pendientes WHERE id = :id")
    suspend fun porId(id: Int): OperacionPendienteEntity?

    @Query("DELETE FROM operaciones_pendientes WHERE idEntidadLocal = :idEntidadLocal")
    suspend fun eliminarDeEntidad(idEntidadLocal: Int)

    @Query("UPDATE operaciones_pendientes SET estado = 'PENDIENTE' WHERE estado = 'ENVIANDO'")
    suspend fun recuperarInterrumpidas()

    @Query("DELETE FROM operaciones_pendientes")
    suspend fun borrarTodo()

    // ---- sync_metadata ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarMetadata(metadata: SyncMetadataEntity)

    @Query("SELECT * FROM sync_metadata WHERE recurso = :recurso")
    fun observarMetadata(recurso: String): Flow<SyncMetadataEntity?>

    @Query("DELETE FROM sync_metadata")
    suspend fun borrarMetadata()
}
