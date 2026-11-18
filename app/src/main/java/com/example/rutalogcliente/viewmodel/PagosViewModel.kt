package com.example.rutalogcliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcliente.data.remote.dto.CobroDto
import com.example.rutalogcliente.data.remote.dto.LiquidacionDto
import com.example.rutalogcliente.data.remote.dto.PerfilDto
import com.example.rutalogcliente.data.repository.PagoRepository
import com.example.rutalogcliente.model.Envio
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Pago del envío con QR (RF-R02): se muestra el QR y se espera la confirmación del banco. */
data class PagoEnvioUi(
    val guia: String? = null,
    val cargando: Boolean = false,
    val cobro: CobroDto? = null,
    val error: String? = null
) {
    val confirmado: Boolean get() = cobro?.estado == "CONFIRMADO"
    val vencido: Boolean get() = cobro?.vencido == true
}

/** "Mi dinero": cuenta para liquidaciones, cobros contraentrega (RF-R04) y liquidaciones (RF-R05). */
data class MiDineroUi(
    val cargando: Boolean = false,
    val perfil: PerfilDto? = null,
    val cobros: List<CobroDto> = emptyList(),
    val liquidaciones: List<LiquidacionDto> = emptyList(),
    val error: String? = null,
    val guardandoCuenta: Boolean = false,
    val mensajeCuenta: String? = null,
    val errorCuenta: String? = null
)

class PagosViewModel(private val repositorio: PagoRepository) : ViewModel() {

    private val _pago = MutableStateFlow(PagoEnvioUi())
    val pago: StateFlow<PagoEnvioUi> = _pago.asStateFlow()

    private val _dinero = MutableStateFlow(MiDineroUi())
    val dinero: StateFlow<MiDineroUi> = _dinero.asStateFlow()

    private var espera: Job? = null

    /** Pide (o recupera) el QR del envío y queda esperando que el banco confirme el pago. */
    fun iniciarPago(envio: Envio, onPagado: () -> Unit) {
        val idRemoto = envio.idRemoto
        if (idRemoto == null) {
            _pago.value = PagoEnvioUi(guia = envio.numeroGuia, error = "Primero sincroniza el envío: el QR se genera en el servidor.")
            return
        }
        espera?.cancel()
        espera = viewModelScope.launch {
            _pago.value = PagoEnvioUi(guia = envio.numeroGuia, cargando = true)
            repositorio.crearQrEnvio(idRemoto)
                .onFailure { e -> _pago.update { it.copy(cargando = false, error = e.message) } }
                .onSuccess { cobro ->
                    _pago.update { it.copy(cargando = false, cobro = cobro) }
                    esperarConfirmacion(cobro.id, onPagado)
                }
        }
    }

    /** Consulta cada 3 s durante 10 min: la API pregunta al banco si el QR ya se pagó. */
    private suspend fun esperarConfirmacion(idCobro: String, onPagado: () -> Unit) {
        val limite = System.currentTimeMillis() + 10 * 60 * 1000
        while (viewModelScope.isActive && System.currentTimeMillis() < limite) {
            if (_pago.value.confirmado) {
                onPagado()
                return
            }
            delay(3000)
            repositorio.consultar(idCobro).onSuccess { cobro ->
                _pago.update { it.copy(cobro = cobro, error = null) }
                if (cobro.vencido == true || cobro.estado == "RECHAZADO") return
            }
        }
    }

    fun detenerPago() {
        espera?.cancel()
    }

    fun cargarMiDinero() {
        viewModelScope.launch {
            _dinero.update { it.copy(cargando = true, error = null) }
            val perfil = repositorio.perfil()
            val cobros = repositorio.cobros()
            val liquidaciones = repositorio.liquidaciones()
            val error = listOf(perfil, cobros, liquidaciones).firstNotNullOfOrNull { it.exceptionOrNull()?.message }
            _dinero.update {
                it.copy(
                    cargando = false,
                    perfil = perfil.getOrNull() ?: it.perfil,
                    cobros = cobros.getOrNull() ?: it.cobros,
                    liquidaciones = liquidaciones.getOrNull() ?: it.liquidaciones,
                    error = error
                )
            }
        }
    }

    fun guardarCuenta(cuenta: String) {
        if (!Regex("^\\d{3}-\\d{7}-\\d$").matches(cuenta.trim())) {
            _dinero.update { it.copy(errorCuenta = "La cuenta tiene la forma 191-2604003-0.", mensajeCuenta = null) }
            return
        }
        viewModelScope.launch {
            _dinero.update { it.copy(guardandoCuenta = true, errorCuenta = null, mensajeCuenta = null) }
            repositorio.guardarCuenta(cuenta)
                .onSuccess { r ->
                    _dinero.update {
                        it.copy(
                            guardandoCuenta = false,
                            perfil = it.perfil?.copy(cuentaBanco = r.cuentaBanco),
                            mensajeCuenta = "Cuenta guardada${r.titular?.let { t -> " · titular $t" } ?: ""}."
                        )
                    }
                }
                .onFailure { e -> _dinero.update { it.copy(guardandoCuenta = false, errorCuenta = e.message) } }
        }
    }

    companion object {
        fun factory(repositorio: PagoRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { PagosViewModel(repositorio) }
        }
    }
}
