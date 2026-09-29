package com.example.rutalogcliente.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.rutalogcliente.model.NumeroGuia
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvioDao {

    @Insert
    suspend fun insertar(envio: Envio): Long

    @Update
    suspend fun actualizar(envio: Envio)

    @Delete
    suspend fun eliminar(envio: Envio)

    @Query("SELECT * FROM envios ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<Envio>>

    @Query("SELECT * FROM envios WHERE id = :id")
    fun obtenerPorId(id: Int): Flow<Envio?>

    /** RF09: la guía se compara sin guiones ni espacios y en mayúsculas. */
    @Query(
        "SELECT * FROM envios " +
            "WHERE REPLACE(REPLACE(UPPER(numeroGuia), '-', ''), ' ', '') = :guiaNormalizada LIMIT 1"
    )
    suspend fun buscarPorGuia(guiaNormalizada: String): Envio?

    @Query("UPDATE envios SET numeroGuia = :numeroGuia WHERE id = :id")
    suspend fun asignarGuia(id: Int, numeroGuia: String)

    /**
     * RF07: inserta el envío y le genera el número de guía a partir del id que asigna
     * Room. Las dos operaciones van en una sola transacción.
     */
    @Transaction
    suspend fun insertarConGuia(envio: Envio): Envio {
        val id = insertar(envio).toInt()
        val guia = NumeroGuia.generar(id)
        asignarGuia(id, guia)
        return envio.copy(id = id, numeroGuia = guia)
    }
}
