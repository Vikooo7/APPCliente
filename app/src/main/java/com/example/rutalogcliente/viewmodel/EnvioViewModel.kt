package com.example.rutalogcliente.viewmodel

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.rutalogcliente.data.repository.EnvioRepository
import com.example.rutalogcliente.data.repository.SeguimientoRepository
import com.example.rutalogcliente.model.CatalogoRutas
import com.example.rutalogcliente.model.DestinatarioForm
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.NumeroGuia
import com.example.rutalogcliente.model.RutaTarifa
import com.example.rutalogcliente.model.Seguimiento
import com.example.rutalogcliente.model.esModificable
import com.example.rutalogcliente.model.estadoEnvio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Seguimiento detallado de una guía (HU-R02). Se pide a la API al abrir el detalle. */
data class SeguimientoUiState(
    val guia: String? = null,
    val cargando: Boolean = false,
    val seguimiento: Seguimiento? = null,
    val error: String? = null
)

/**
 * Estado y acciones de las pantallas de envíos. No toca Room ni Retrofit: todo pasa por
 * los repositorios. Después de cada escritura llama a [programarSincronizacion] para que
 * WorkManager la envíe cuando haya conexión.
 */
class EnvioViewModel(
    private val repositorio: EnvioRepository,
    private val seguimientoRepository: SeguimientoRepository,
    private val programarSincronizacion: () -> Unit
) : ViewModel() {

    /** Lista leída de Room; se actualiza sola al guardar, editar, eliminar o sincronizar. */
    val envios: StateFlow<List<Envio>> = repositorio.observarEnvios()
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

    fun obtenerPorId(id: Int): Flow<Envio?> = repositorio.observarPorId(id)

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

    /** RF06 + RF07 + RF08 + RF12 + HU-R01: guarda el envío en Room y lo deja en la cola de sincronización. */
    fun registrar(
        ruta: RutaTarifa?,
        pesoTexto: String,
        destinatario: DestinatarioForm,
        onError: (String) -> Unit,
        onRegistrado: (Envio) -> Unit
    ) {
        val error = validar(ruta, pesoTexto) ?: destinatario.validar()
        if (error != null || ruta == null) {
            onError(error ?: "Revisa los datos del envío.")
            return
        }
        val peso = leerPeso(pesoTexto) ?: return
        viewModelScope.launch {
            val creado = repositorio.registrar(ruta, peso, destinatario.aDestinatario(), destinatario.montoContraentrega())
            programarSincronizacion()
            onRegistrado(creado)
        }
    }

    /** Editar ruta, peso o destinatario: solo mientras el envío siga pendiente de recojo. */
    fun actualizar(
        envio: Envio,
        ruta: RutaTarifa?,
        pesoTexto: String,
        destinatario: DestinatarioForm,
        onError: (String) -> Unit,
        onActualizado: () -> Unit
    ) {
        if (!envio.esModificable) {
            onError("Solo puedes editar envíos que siguen pendientes de recojo.")
            return
        }
        val error = validar(ruta, pesoTexto) ?: destinatario.validar()
        if (error != null || ruta == null) {
            onError(error ?: "Revisa los datos del envío.")
            return
        }
        val peso = leerPeso(pesoTexto) ?: return
        viewModelScope.launch {
            repositorio.actualizar(envio.id, ruta, peso, destinatario.aDestinatario(), destinatario.montoContraentrega())
            programarSincronizacion()
            onActualizado()
        }
    }

    private val _seguimiento = MutableStateFlow(SeguimientoUiState())
    val seguimiento: StateFlow<SeguimientoUiState> = _seguimiento.asStateFlow()

    /**
     * HU-R02: pide a la API los eventos y la prueba de entrega. Un envío que todavía no llegó
     * al servidor (registrado sin conexión) solo tiene lo que muestra Room.
     */
    /** Fotos y firmas ya descargadas: no se vuelven a pedir al girar la pantalla o al actualizar. */
    private val imagenes = mutableMapOf<String, ImageBitmap>()

    /** HU-R02: foto o firma de la prueba de entrega, lista para mostrarse (null si no se pudo descargar). */
    suspend fun cargarImagen(url: String): ImageBitmap? {
        imagenes[url]?.let { return it }
        val bytes = seguimientoRepository.imagen(url) ?: return null
        val imagen = withContext(Dispatchers.Default) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } ?: return null
        imagenes[url] = imagen
        return imagen
    }

    fun cargarSeguimiento(envio: Envio) {
        if (envio.idRemoto == null) {
            _seguimiento.value = SeguimientoUiState(
                guia = envio.numeroGuia,
                error = "El seguimiento detallado estará disponible cuando el envío se sincronice."
            )
            return
        }
        _seguimiento.update { anterior ->
            if (anterior.guia == envio.numeroGuia) anterior.copy(cargando = true, error = null)
            else SeguimientoUiState(guia = envio.numeroGuia, cargando = true)
        }
        viewModelScope.launch {
            seguimientoRepository.cargar(envio.numeroGuia)
                .onSuccess { datos ->
                    _seguimiento.update { it.copy(cargando = false, seguimiento = datos, error = null) }
                }
                .onFailure { e ->
                    _seguimiento.update { it.copy(cargando = false, error = e.message) }
                }
        }
    }

    fun eliminar(envio: Envio, onError: (String) -> Unit, onEliminado: () -> Unit) {
        if (!envio.esModificable) {
            onError("Solo puedes eliminar envíos que siguen pendientes de recojo.")
            return
        }
        viewModelScope.launch {
            repositorio.eliminar(envio.id)
            programarSincronizacion()
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
            val envio = repositorio.buscarPorGuia(normalizada)
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
        fun factory(
            repositorio: EnvioRepository,
            seguimientoRepository: SeguimientoRepository,
            programarSincronizacion: () -> Unit
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { EnvioViewModel(repositorio, seguimientoRepository, programarSincronizacion) }
        }
    }
}
