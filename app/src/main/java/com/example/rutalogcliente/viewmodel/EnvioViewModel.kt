package com.example.rutalogcliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcliente.data.local.Envio
import com.example.rutalogcliente.data.local.EnvioDao
import com.example.rutalogcliente.model.CatalogoRutas
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.NumeroGuia
import com.example.rutalogcliente.model.RutaTarifa
import com.example.rutalogcliente.model.esModificable
import com.example.rutalogcliente.model.estadoEnvio
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EnvioViewModel(private val envioDao: EnvioDao) : ViewModel() {

    /** Lista completa leída de Room; se actualiza sola al insertar, editar o eliminar. */
    val envios: StateFlow<List<Envio>> = envioDao.obtenerTodos()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _texto = MutableStateFlow("")
    val texto: StateFlow<String> = _texto.asStateFlow()

    private val _filtroEstado = MutableStateFlow<EstadoEnvio?>(null)
    val filtroEstado: StateFlow<EstadoEnvio?> = _filtroEstado.asStateFlow()

    val enviosFiltrados: StateFlow<List<Envio>> = combine(envios, _texto, _filtroEstado) { lista, texto, estado ->
        val buscado = texto.trim()
        val guiaBuscada = NumeroGuia.normalizar(buscado)
        lista.filter { envio ->
            (estado == null || envio.estadoEnvio == estado) &&
                (buscado.isEmpty() ||
                    NumeroGuia.normalizar(envio.numeroGuia).contains(guiaBuscada) ||
                    envio.ruta.contains(buscado, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _errorBusqueda = MutableStateFlow<String?>(null)
    val errorBusqueda: StateFlow<String?> = _errorBusqueda.asStateFlow()

    fun buscarTexto(texto: String) { _texto.value = texto }
    fun filtrarPorEstado(estado: EstadoEnvio?) { _filtroEstado.value = estado }

    fun obtenerPorId(id: Int): Flow<Envio?> = envioDao.obtenerPorId(id)

    /** RF08 */
    fun calcularCosto(pesoKg: Double, ruta: RutaTarifa): Double =
        CatalogoRutas.calcularCosto(pesoKg, ruta.tarifaPorKg)

    fun leerPeso(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

    /** RF10 y demás validaciones del formulario. Devuelve null si todo está bien. */
    fun validar(ruta: RutaTarifa?, pesoTexto: String): String? {
        val peso = leerPeso(pesoTexto)
        return when {
            ruta == null -> "Selecciona la ruta del envío."
            pesoTexto.isBlank() -> "Ingresa el peso de la carga."
            peso == null -> "El peso debe ser un número, por ejemplo 12.5."
            peso <= 0.0 -> "El peso debe ser mayor que 0 kg."
            peso > 30_000.0 -> "El peso máximo por envío es 30 000 kg."
            else -> null
        }
    }

    /** RF06 + RF07 + RF08: registra el envío en Room y genera su número de guía. */
    fun registrar(
        ruta: RutaTarifa?,
        pesoTexto: String,
        onError: (String) -> Unit,
        onRegistrado: (Envio) -> Unit
    ) {
        val error = validar(ruta, pesoTexto)
        if (error != null || ruta == null) {
            onError(error ?: "Revisa los datos del envío.")
            return
        }
        val peso = leerPeso(pesoTexto) ?: return
        viewModelScope.launch {
            val creado = envioDao.insertarConGuia(
                Envio(
                    numeroGuia = "",
                    ruta = ruta.nombre,
                    pesoKg = peso,
                    costoEnvio = calcularCosto(peso, ruta),
                    estado = EstadoEnvio.PENDIENTE.codigo,
                    transportistaAsignado = null
                )
            )
            onRegistrado(creado)
        }
    }

    /** Editar ruta o peso: solo mientras el envío siga pendiente de recojo. */
    fun actualizar(
        envio: Envio,
        ruta: RutaTarifa?,
        pesoTexto: String,
        onError: (String) -> Unit,
        onActualizado: () -> Unit
    ) {
        if (!envio.esModificable) {
            onError("Solo puedes editar envíos que siguen pendientes de recojo.")
            return
        }
        val error = validar(ruta, pesoTexto)
        if (error != null || ruta == null) {
            onError(error ?: "Revisa los datos del envío.")
            return
        }
        val peso = leerPeso(pesoTexto) ?: return
        viewModelScope.launch {
            envioDao.actualizar(
                envio.copy(ruta = ruta.nombre, pesoKg = peso, costoEnvio = calcularCosto(peso, ruta))
            )
            onActualizado()
        }
    }

    fun eliminar(envio: Envio, onError: (String) -> Unit, onEliminado: () -> Unit) {
        if (!envio.esModificable) {
            onError("Solo puedes eliminar envíos que siguen pendientes de recojo.")
            return
        }
        viewModelScope.launch {
            envioDao.eliminar(envio)
            onEliminado()
        }
    }

    /** RF09 */
    fun buscarPorGuia(texto: String, onEncontrado: (Envio) -> Unit) {
        val normalizada = NumeroGuia.normalizar(texto)
        if (normalizada.isEmpty()) {
            _errorBusqueda.value = "Ingresa un número de guía."
            return
        }
        viewModelScope.launch {
            val envio = envioDao.buscarPorGuia(normalizada)
            if (envio == null) {
                _errorBusqueda.value = "No encontramos la guía ${texto.trim().uppercase()}."
            } else {
                _errorBusqueda.value = null
                onEncontrado(envio)
            }
        }
    }

    fun limpiarErrorBusqueda() {
        _errorBusqueda.value = null
    }

    companion object {
        fun factory(envioDao: EnvioDao): ViewModelProvider.Factory = viewModelFactory {
            initializer { EnvioViewModel(envioDao) }
        }
    }
}
