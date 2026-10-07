package com.example.rutalogcliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcliente.data.local.entities.TipoOperacion
import com.example.rutalogcliente.data.repository.ResultadoSync
import com.example.rutalogcliente.data.repository.SyncRepository
import com.example.rutalogcliente.model.EstadoSincronizacion
import com.example.rutalogcliente.util.ConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** Una operación de la cola, lista para mostrarse en la pantalla de sincronización. */
data class OperacionUi(
    val id: Int,
    val descripcion: String,
    val numeroGuia: String,
    val estado: EstadoSincronizacion,
    val intentos: Int,
    val mensajeError: String?,
    val fechaRegistro: Long
)

data class SyncUiState(
    val enLinea: Boolean = true,
    val sincronizando: Boolean = false,
    val operaciones: List<OperacionUi> = emptyList(),
    /** Fecha de la última sincronización exitosa (milisegundos), o null si nunca hubo. */
    val ultimaSincronizacion: Long? = null,
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false,
    val simularErrorServidor: Boolean = false
) {
    val pendientes: Int get() = operaciones.count { it.estado != EstadoSincronizacion.ERROR }
    val errores: Int get() = operaciones.count { it.estado == EstadoSincronizacion.ERROR }
}

/** RF13 y RF14: sincronización manual y estado de la cola (pendientes, errores, última fecha). */
class SyncViewModel(
    private val repositorio: SyncRepository,
    conectividad: ConnectivityObserver,
    private val simularError: AtomicBoolean
) : ViewModel() {

    private val _estado = MutableStateFlow(
        SyncUiState(enLinea = conectividad.hayConexion(), simularErrorServidor = simularError.get())
    )
    val estado: StateFlow<SyncUiState> = _estado.asStateFlow()

    /** La app indica si hay sesión; sin sesión no se sincroniza. */
    var haySesion: () -> Boolean = { false }

    init {
        viewModelScope.launch {
            repositorio.observarOperaciones().collect { lista ->
                _estado.update { actual ->
                    actual.copy(
                        operaciones = lista.map { fila ->
                            val op = fila.operacion
                            OperacionUi(
                                id = op.id,
                                descripcion = when (op.tipoOperacion) {
                                    TipoOperacion.CREAR -> "Registrar envío"
                                    TipoOperacion.ACTUALIZAR -> "Modificar envío"
                                    else -> "Eliminar envío"
                                },
                                numeroGuia = fila.numeroGuia ?: "—",
                                estado = EstadoSincronizacion.desde(op.estado),
                                intentos = op.intentos,
                                mensajeError = op.mensajeError,
                                fechaRegistro = op.fechaRegistro
                            )
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            repositorio.observarUltimaSincronizacion().collect { fecha ->
                _estado.update { it.copy(ultimaSincronizacion = fecha) }
            }
        }
        viewModelScope.launch {
            conectividad.enLinea.collect { enLinea ->
                _estado.update { it.copy(enLinea = enLinea) }
            }
        }
        // Al recuperar la conexión se sincroniza sola (además del SyncWorker).
        viewModelScope.launch {
            conectividad.enLinea.drop(1).collect { enLinea ->
                if (enLinea && haySesion()) sincronizar(silencioso = true)
            }
        }
    }

    /** Botón "Sincronizar ahora" y descarga inicial después del login. */
    fun sincronizar(silencioso: Boolean = false) {
        if (_estado.value.sincronizando) return
        if (!_estado.value.enLinea) {
            if (!silencioso) {
                mostrar("Sin conexión. Tus cambios están guardados y se enviarán al recuperarla.", esError = true)
            }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(sincronizando = true, mensaje = null) }
            val resultado = repositorio.sincronizar()
            _estado.update { it.copy(sincronizando = false) }
            if (!silencioso || resultado.error != null || resultado.rechazadas > 0) {
                mostrar(describir(resultado), esError = resultado.error != null || resultado.rechazadas > 0)
            }
        }
    }

    fun reintentar(idOperacion: Int) {
        viewModelScope.launch {
            repositorio.reintentar(idOperacion)
            sincronizar()
        }
    }

    /** Descarta el cambio local rechazado y vuelve a traer los datos del servidor. */
    fun descartar(idOperacion: Int) {
        viewModelScope.launch {
            repositorio.descartar(idOperacion)
            sincronizar(silencioso = true)
        }
    }

    /** Prueba de la exposición: hace que la API responda con error 503. */
    fun cambiarSimulacionError(activa: Boolean) {
        simularError.set(activa)
        _estado.update { it.copy(simularErrorServidor = activa) }
    }

    fun limpiarMensaje() {
        _estado.update { it.copy(mensaje = null) }
    }

    private fun mostrar(texto: String, esError: Boolean) {
        _estado.update { it.copy(mensaje = texto, mensajeEsError = esError) }
    }

    private fun describir(resultado: ResultadoSync): String {
        val partes = mutableListOf<String>()
        if (resultado.enviadas > 0) partes += "${resultado.enviadas} enviada(s)"
        if (resultado.rechazadas > 0) partes += "${resultado.rechazadas} rechazada(s) por el servidor"
        return when {
            resultado.error != null ->
                "No se completó: ${resultado.error}" +
                    if (partes.isEmpty()) "" else " (${partes.joinToString(", ")})"

            partes.isEmpty() -> "Todo al día. ${resultado.descargados} envíos descargados."
            else -> "Sincronizado: ${partes.joinToString(", ")}. ${resultado.descargados} envíos descargados."
        }
    }

    companion object {
        fun factory(
            repositorio: SyncRepository,
            conectividad: ConnectivityObserver,
            simularError: AtomicBoolean
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { SyncViewModel(repositorio, conectividad, simularError) }
        }
    }
}
