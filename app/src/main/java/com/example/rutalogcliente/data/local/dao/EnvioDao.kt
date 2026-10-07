package com.example.rutalogcliente.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.rutalogcliente.data.local.entities.EnvioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvioDao {

    @Insert
    suspend fun insertar(envio: EnvioEntity): Long

    @Update
    suspend fun actualizar(envio: EnvioEntity)

    @Delete
    suspend fun eliminar(envio: EnvioEntity)

    /** Los eliminados sin conexión se ocultan aunque sigan en la tabla hasta sincronizar. */
    @Query("SELECT * FROM envios WHERE eliminadoLocal = 0 ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<EnvioEntity>>

    @Query("SELECT * FROM envios WHERE id = :id")
    fun observarPorId(id: Int): Flow<EnvioEntity?>

    @Query("SELECT * FROM envios WHERE id = :id")
    suspend fun porId(id: Int): EnvioEntity?

    @Query("SELECT * FROM envios")
    suspend fun todos(): List<EnvioEntity>

    @Query("SELECT numeroGuia FROM envios")
    suspend fun guias(): List<String>

    /** RF09: la guía se compara sin guiones ni espacios y en mayúsculas. */
    @Query(
        "SELECT * FROM envios WHERE eliminadoLocal = 0 " +
            "AND REPLACE(REPLACE(UPPER(numeroGuia), '-', ''), ' ', '') = :guiaNormalizada LIMIT 1"
    )
    suspend fun buscarPorGuia(guiaNormalizada: String): EnvioEntity?

    /** Si la app se cerró a mitad de un envío, esos registros vuelven a quedar pendientes. */
    @Query("UPDATE envios SET estadoSync = 'PENDIENTE' WHERE estadoSync = 'ENVIANDO'")
    suspend fun recuperarInterrumpidos()

    @Query("DELETE FROM envios")
    suspend fun borrarTodo()
}
