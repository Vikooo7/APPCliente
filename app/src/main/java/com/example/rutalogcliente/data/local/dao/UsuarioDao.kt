package com.example.rutalogcliente.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.rutalogcliente.data.local.entities.UsuarioEntity

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: UsuarioEntity)

    /** Perfil guardado en el último inicio de sesión. */
    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun actual(): UsuarioEntity?

    @Query("DELETE FROM usuarios")
    suspend fun borrarTodo()
}
