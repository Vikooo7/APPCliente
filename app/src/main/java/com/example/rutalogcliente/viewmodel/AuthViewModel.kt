package com.example.rutalogcliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcliente.data.local.Usuario
import com.example.rutalogcliente.data.local.UsuarioDao
import com.example.rutalogcliente.model.RolUsuario
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val cargando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    /** Aumenta con cada intento fallido para animar el formulario aunque el error se repita. */
    val intentosFallidos: Int = 0,
    val usuario: Usuario? = null,
    /** Correo recién registrado: se usa para rellenar el Login. */
    val correoSugerido: String = ""
)

class AuthViewModel(private val usuarioDao: UsuarioDao) : ViewModel() {

    private val _estado = MutableStateFlow(AuthUiState())
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    /** RFA03 + RFA04 + RFA05 */
    fun login(correo: String, clave: String, onExito: () -> Unit) {
        val correoLimpio = correo.trim().lowercase()
        if (correoLimpio.isEmpty() || clave.isEmpty()) {
            fallar("Ingresa tu correo y tu contraseña.")
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null, mensaje = null) }
            delay(500)
            val usuario = usuarioDao.login(correoLimpio, clave)
            if (usuario == null) {
                val existe = usuarioDao.buscarPorCorreo(correoLimpio) != null
                fallar(
                    if (existe) "La contraseña es incorrecta."
                    else "No existe una cuenta con ese correo. Regístrate primero."
                )
            } else {
                _estado.update { it.copy(cargando = false, usuario = usuario) }
                onExito()
            }
        }
    }

    /** RFA02: el rol queda fijo como "cliente" en esta app. */
    fun registrar(
        nombre: String,
        correo: String,
        clave: String,
        confirmacion: String,
        onExito: () -> Unit
    ) {
        val correoLimpio = correo.trim().lowercase()
        val error = when {
            nombre.trim().length < 3 -> "Ingresa tu nombre o el de tu empresa."
            !correoLimpio.contains("@") || !correoLimpio.substringAfter("@").contains(".") ->
                "Ingresa un correo válido."
            clave.length < 4 -> "La contraseña debe tener al menos 4 caracteres."
            clave != confirmacion -> "Las contraseñas no coinciden."
            else -> null
        }
        if (error != null) {
            fallar(error)
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            if (usuarioDao.buscarPorCorreo(correoLimpio) != null) {
                fallar("Ya existe una cuenta con ese correo.")
                return@launch
            }
            usuarioDao.registrar(
                Usuario(
                    nombre = nombre.trim(),
                    correo = correoLimpio,
                    clave = clave,
                    rol = RolUsuario.CLIENTE.valor
                )
            )
            _estado.update {
                it.copy(
                    cargando = false,
                    mensaje = "Cuenta creada. Ya puedes iniciar sesión.",
                    correoSugerido = correoLimpio
                )
            }
            onExito()
        }
    }

    fun limpiarMensajes() {
        _estado.update { it.copy(error = null, mensaje = null) }
    }

    fun cerrarSesion() {
        _estado.value = AuthUiState()
    }

    private fun fallar(mensaje: String) {
        _estado.update {
            it.copy(cargando = false, error = mensaje, intentosFallidos = it.intentosFallidos + 1)
        }
    }

    companion object {
        fun factory(usuarioDao: UsuarioDao): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(usuarioDao) }
        }
    }
}
