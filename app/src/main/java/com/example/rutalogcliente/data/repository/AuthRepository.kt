package com.example.rutalogcliente.data.repository

import androidx.room.withTransaction
import com.example.rutalogcliente.data.local.AppDatabase
import com.example.rutalogcliente.data.local.SesionSegura
import com.example.rutalogcliente.data.mapper.aEntidad
import com.example.rutalogcliente.data.mapper.aModelo
import com.example.rutalogcliente.data.remote.ResultadoApi
import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.remote.dto.LoginRequest
import com.example.rutalogcliente.data.remote.llamarApi
import com.example.rutalogcliente.model.RolUsuario
import com.example.rutalogcliente.model.Usuario

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data class Error(val mensaje: String) : ResultadoLogin
}

/**
 * Autenticación mediante la API REST.
 * - El token se guarda cifrado (SesionSegura) y el perfil en la tabla usuarios.
 * - Acceso sin conexión: quien ya inició sesión conserva su sesión y entra aunque no haya red.
 *   Un inicio de sesión nuevo siempre necesita conexión, porque la clave no se guarda en el teléfono.
 */
class AuthRepository(
    private val api: ApiService,
    private val db: AppDatabase,
    private val sesion: SesionSegura
) {

    /** Usuario con sesión activa, o null si hay que iniciar sesión. */
    suspend fun usuarioActual(): Usuario? =
        if (sesion.token == null) null else db.usuarioDao().actual()?.aModelo()

    suspend fun login(correo: String, clave: String): ResultadoLogin =
        when (val resultado = llamarApi { api.login(LoginRequest(correo, clave)) }) {
            is ResultadoApi.Exito -> {
                val perfil = resultado.valor.usuario
                if (perfil.rol != RolUsuario.CLIENTE.valor) {
                    ResultadoLogin.Error("Esta cuenta no es de cliente. Usa la App Operador.")
                } else {
                    db.withTransaction {
                        val anterior = db.usuarioDao().actual()
                        // Si entra otra persona en este teléfono, no debe ver los datos de la anterior.
                        if (anterior != null && anterior.id != perfil.id) {
                            db.envioDao().borrarTodo()
                            db.operacionPendienteDao().borrarTodo()
                            db.operacionPendienteDao().borrarMetadata()
                        }
                        db.usuarioDao().borrarTodo()
                        db.usuarioDao().guardar(perfil.aEntidad())
                    }
                    sesion.token = resultado.valor.token
                    ResultadoLogin.Exito(perfil.aEntidad().aModelo())
                }
            }

            is ResultadoApi.Rechazo -> ResultadoLogin.Error(resultado.mensaje)

            is ResultadoApi.Fallo -> ResultadoLogin.Error(
                if (resultado.sinConexion) {
                    "Sin conexión. Necesitas internet para iniciar sesión."
                } else {
                    resultado.mensaje
                }
            )
        }

    /** Cierra la sesión. Los datos locales se conservan por si el mismo usuario vuelve a entrar. */
    fun cerrarSesion() {
        sesion.cerrar()
    }
}
