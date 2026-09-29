package com.example.rutalogcliente.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UsuarioDao {

    /** RFA02: registrar un nuevo usuario. */
    @Insert
    suspend fun registrar(usuario: Usuario)

    /** RFA03: iniciar sesión validando correo y clave. */
    @Query("SELECT * FROM usuarios WHERE correo = :correo AND clave = :clave LIMIT 1")
    suspend fun login(correo: String, clave: String): Usuario?

    /** Evita correos repetidos y permite distinguir los errores de RFA04. */
    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarPorCorreo(correo: String): Usuario?
}
