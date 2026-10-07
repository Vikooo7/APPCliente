package com.example.rutalogcliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcliente.data.repository.AuthRepository
import com.example.rutalogcliente.data.repository.ResultadoLogin
import com.example.rutalogcliente.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    /** true mientras se revisa si hay una sesión guardada (al abrir la app). */
    val comprobandoSesion: Boolean = true,
    val cargando: Boolean = false,
    val error: String? = null,
    /** Aumenta con cada intento fallido para animar el formulario aunque el error se repita. */
    val intentosFallidos: Int = 0,
    val usuario: Usuario? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(AuthUiState())
    val estado: StateFlow<AuthUiState> = _estado.asStateFlow()

    init {
        // Acceso sin conexión: si ya había sesión, se entra directo sin llamar a la API.
        viewModelScope.launch {
            val usuario = authRepository.usuarioActual()
            _estado.update { it.copy(comprobandoSesion = false, usuario = usuario) }
        }
    }

    /** Inicia sesión contra la API REST. */
    fun login(correo: String, clave: String, onExito: () -> Unit) {
        val correoLimpio = correo.trim().lowercase()
        if (correoLimpio.isEmpty() || clave.isEmpty()) {
            fallar("Ingresa tu correo y tu contraseña.")
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            when (val resultado = authRepository.login(correoLimpio, clave)) {
                is ResultadoLogin.Exito -> {
                    _estado.update { it.copy(cargando = false, usuario = resultado.usuario) }
                    onExito()
                }

                is ResultadoLogin.Error -> fallar(resultado.mensaje)
            }
        }
    }

    fun limpiarMensajes() {
        _estado.update { it.copy(error = null) }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
        _estado.value = AuthUiState(comprobandoSesion = false)
    }

    private fun fallar(mensaje: String) {
        _estado.update {
            it.copy(cargando = false, error = mensaje, intentosFallidos = it.intentosFallidos + 1)
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AuthViewModel(authRepository) }
        }
    }
}
