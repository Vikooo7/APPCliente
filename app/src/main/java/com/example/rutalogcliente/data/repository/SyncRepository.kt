package com.example.rutalogcliente.data.repository

import androidx.room.withTransaction
import com.example.rutalogcliente.data.local.AppDatabase
import com.example.rutalogcliente.data.local.dao.OperacionConGuia
import com.example.rutalogcliente.data.local.entities.EnvioEntity
import com.example.rutalogcliente.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcliente.data.local.entities.SyncMetadataEntity
import com.example.rutalogcliente.data.local.entities.TipoOperacion
import com.example.rutalogcliente.data.mapper.aEntidad
import com.example.rutalogcliente.data.remote.ResultadoApi
import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.remote.dto.ActualizarEnvioRequest
import com.example.rutalogcliente.data.remote.dto.CrearEnvioRequest
import com.example.rutalogcliente.data.remote.dto.EnvioDto
import com.example.rutalogcliente.data.remote.llamarApi
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.EstadoSincronizacion
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Resumen de una sincronización, para mostrarlo en pantalla. */
data class ResultadoSync(
    val enviadas: Int = 0,
    val rechazadas: Int = 0,
    val descargados: Int = 0,
    /** null si todo salió bien; si no, el motivo por el que se detuvo. */
    val error: String? = null,
    /** La API respondió 401: hay que volver a iniciar sesión (los cambios siguen en la cola). */
    val sesionVencida: Boolean = false,
    /** Envíos que cambiaron de estado en el servidor (recogido, en reparto, entregado...). */
    val cambios: List<CambioEstado> = emptyList(),
    /** Envíos nuevos cuya guía ya existía en el servidor y recibieron otra. */
    val guiasCambiadas: List<GuiaCambiada> = emptyList()
)

/** Un envío del cliente que avanzó en el servidor desde la última sincronización. */
data class CambioEstado(val numeroGuia: String, val estado: EstadoEnvio)

/** La guía que generó el celular ya era de otro envío: el servidor asignó [nueva]. */
data class GuiaCambiada(val anterior: String, val nueva: String)

/** Solo al registrar: si el servidor devuelve otra guía, la que generó el celular ya existía. */
internal fun cambioDeGuia(tipoOperacion: String, guiaLocal: String, guiaServidor: String): GuiaCambiada? =
    if (tipoOperacion == TipoOperacion.CREAR && !guiaLocal.equals(guiaServidor, ignoreCase = true)) {
        GuiaCambiada(guiaLocal, guiaServidor)
    } else {
        null
    }

/**
 * Sincronización con la API REST (RF11 a RF15):
 * 1. Envía, en orden, las operaciones de la cola local. Cada una lleva su UUID, así el
 *    servidor no la aplica dos veces aunque se reenvíe.
 * 2. Descarga los envíos autorizados y actualiza Room, sin tocar los registros que todavía
 *    tienen cambios locales por enviar.
 *
 * Si el servidor rechaza una operación (validación o conflicto), el registro local se conserva
 * con estado ERROR y el motivo, para que el usuario decida si reintenta o descarta su cambio.
 */
class SyncRepository(
    private val db: AppDatabase,
    private val api: ApiService,
    /** Avisa en el celular cuando un envío cambia de estado (lo conecta el AppContainer). */
    private val alCambiarEstados: (List<CambioEstado>) -> Unit = {},
    /** Avisa cuando el servidor tuvo que asignar otra guía a un envío nuevo. */
    private val alCambiarGuias: (List<GuiaCambiada>) -> Unit = {}
) {

    private val envioDao = db.envioDao()
    private val operacionDao = db.operacionPendienteDao()
    private val gson = Gson()

    /** Evita que el botón manual y el SyncWorker sincronicen al mismo tiempo. */
    private val candado = Mutex()

    fun observarOperaciones(): Flow<List<OperacionConGuia>> = operacionDao.observarConGuia()

    fun observarUltimaSincronizacion(): Flow<Long?> =
        operacionDao.observarMetadata(RECURSO_ENVIOS).map { it?.ultimaSincronizacionExitosa }

    suspend fun sincronizar(): ResultadoSync = candado.withLock {
        // Si la app se cerró a mitad de un envío, esas operaciones vuelven a la cola.
        operacionDao.recuperarInterrumpidas()
        envioDao.recuperarInterrumpidos()

        var enviadas = 0
        var rechazadas = 0
        val guiasCambiadas = mutableListOf<GuiaCambiada>()

        for (operacion in operacionDao.porEnviar()) {
            val envio = envioDao.porId(operacion.idEntidadLocal)
            if (envio == null) {
                operacionDao.eliminar(operacion)
                continue
            }

            marcar(operacion, envio, EstadoSincronizacion.ENVIANDO)

            when (val resultado = enviar(operacion, envio)) {
                is ResultadoApi.Exito -> {
                    aplicarExito(operacion, envio.id, resultado.valor)?.let { guiasCambiadas += it }
                    enviadas++
                }

                is ResultadoApi.Rechazo -> {
                    if (resultado.http == 401) {
                        marcar(operacion, envio, EstadoSincronizacion.PENDIENTE)
                        return@withLock ResultadoSync(enviadas, rechazadas, error = resultado.mensaje, sesionVencida = true)
                    }
                    // Validación o conflicto: no se pierde nada, queda marcado con el motivo.
                    db.withTransaction {
                        operacionDao.actualizar(
                            operacion.copy(
                                estado = EstadoSincronizacion.ERROR.name,
                                intentos = operacion.intentos + 1,
                                mensajeError = resultado.mensaje
                            )
                        )
                        envioDao.porId(envio.id)?.let {
                            envioDao.actualizar(
                                it.copy(estadoSync = EstadoSincronizacion.ERROR.name, mensajeError = resultado.mensaje)
                            )
                        }
                    }
                    rechazadas++
                }

                is ResultadoApi.Fallo -> {
                    // Sin conexión o servidor caído: la operación sigue en la cola para otro intento.
                    db.withTransaction {
                        operacionDao.actualizar(
                            operacion.copy(
                                estado = EstadoSincronizacion.PENDIENTE.name,
                                intentos = operacion.intentos + 1,
                                mensajeError = resultado.mensaje
                            )
                        )
                        envioDao.porId(envio.id)?.let {
                            envioDao.actualizar(it.copy(estadoSync = EstadoSincronizacion.PENDIENTE.name))
                        }
                    }
                    return@withLock ResultadoSync(enviadas, rechazadas, error = resultado.mensaje)
                }
            }
        }

        // Se avisa aunque la descarga posterior falle: el cambio de guía ya quedó guardado.
        if (guiasCambiadas.isNotEmpty()) alCambiarGuias(guiasCambiadas)

        when (val descarga = llamarApi { api.obtenerEnvios() }) {
            is ResultadoApi.Exito -> {
                val cambios = guardarDescarga(descarga.valor.datos, descarga.valor.servidorAhora)
                if (cambios.isNotEmpty()) alCambiarEstados(cambios)
                ResultadoSync(enviadas, rechazadas, descargados = descarga.valor.datos.size, cambios = cambios, guiasCambiadas = guiasCambiadas)
            }

            is ResultadoApi.Rechazo -> ResultadoSync(
                enviadas, rechazadas, error = descarga.mensaje, sesionVencida = descarga.http == 401, guiasCambiadas = guiasCambiadas
            )
            is ResultadoApi.Fallo -> ResultadoSync(enviadas, rechazadas, error = descarga.mensaje, guiasCambiadas = guiasCambiadas)
        }
    }

    /** El usuario corrigió el dato o quiere volver a intentarlo: la operación regresa a la cola. */
    suspend fun reintentar(idOperacion: Int) {
        db.withTransaction {
            val operacion = operacionDao.porId(idOperacion) ?: return@withTransaction
            operacionDao.actualizar(
                operacion.copy(estado = EstadoSincronizacion.PENDIENTE.name, mensajeError = null)
            )
            envioDao.porId(operacion.idEntidadLocal)?.let {
                envioDao.actualizar(it.copy(estadoSync = EstadoSincronizacion.PENDIENTE.name, mensajeError = null))
            }
        }
    }

    /**
     * El usuario renuncia a su cambio local y acepta lo que diga el servidor.
     * La siguiente descarga deja el registro igual que en el servidor.
     */
    suspend fun descartar(idOperacion: Int) {
        db.withTransaction {
            val operacion = operacionDao.porId(idOperacion) ?: return@withTransaction
            operacionDao.eliminarDeEntidad(operacion.idEntidadLocal)
            val envio = envioDao.porId(operacion.idEntidadLocal) ?: return@withTransaction
            if (envio.idRemoto == null) {
                // El servidor nunca aceptó este envío: se quita del teléfono.
                envioDao.eliminar(envio)
            } else {
                envioDao.actualizar(
                    envio.copy(
                        eliminadoLocal = false,
                        estadoSync = EstadoSincronizacion.SINCRONIZADO.name,
                        mensajeError = null
                    )
                )
            }
        }
    }

    private suspend fun marcar(operacion: OperacionPendienteEntity, envio: EnvioEntity, estado: EstadoSincronizacion) {
        db.withTransaction {
            operacionDao.actualizar(operacion.copy(estado = estado.name))
            envioDao.actualizar(envio.copy(estadoSync = estado.name))
        }
    }

    /** Envía una operación. Devuelve el envío del servidor, o null si era una eliminación. */
    private suspend fun enviar(operacion: OperacionPendienteEntity, envio: EnvioEntity): ResultadoApi<EnvioDto?> {
        val datos = try {
            gson.fromJson(operacion.payload, PayloadEnvio::class.java)
        } catch (e: Exception) {
            null
        } ?: PayloadEnvio(envio.uuid, envio.numeroGuia, envio.ruta, envio.pesoKg)

        return when (operacion.tipoOperacion) {
            TipoOperacion.CREAR -> llamarApi {
                api.crearEnvio(
                    CrearEnvioRequest(
                        uuidOperacion = operacion.uuidOperacion,
                        uuid = envio.uuid,
                        numeroGuia = datos.numeroGuia,
                        ruta = datos.ruta,
                        pesoKg = datos.pesoKg,
                        destinatarioNombre = datos.destinatarioNombre,
                        destinatarioTelefono = datos.destinatarioTelefono,
                        destinatarioDireccion = datos.destinatarioDireccion,
                        montoContraentrega = datos.montoContraentrega
                    )
                )
            }

            TipoOperacion.ACTUALIZAR -> {
                val idRemoto = envio.idRemoto
                    ?: return ResultadoApi.Rechazo(0, "SIN_REGISTRO", "Primero debe aceptarse el registro de este envío.")
                llamarApi {
                    api.actualizarEnvio(
                        idRemoto,
                        ActualizarEnvioRequest(
                            uuidOperacion = operacion.uuidOperacion,
                            ruta = datos.ruta,
                            pesoKg = datos.pesoKg,
                            version = envio.version,
                            destinatarioNombre = datos.destinatarioNombre,
                            destinatarioTelefono = datos.destinatarioTelefono,
                            destinatarioDireccion = datos.destinatarioDireccion,
                            montoContraentrega = datos.montoContraentrega
                        )
                    )
                }
            }

            else -> {
                val idRemoto = envio.idRemoto ?: return ResultadoApi.Exito(null)
                when (val r = llamarApi { api.eliminarEnvio(idRemoto, operacion.uuidOperacion) }) {
                    is ResultadoApi.Exito -> ResultadoApi.Exito(null)
                    // Si el servidor ya no lo tiene, el resultado es el que se buscaba.
                    is ResultadoApi.Rechazo -> if (r.http == 404) ResultadoApi.Exito(null) else r
                    is ResultadoApi.Fallo -> r
                }
            }
        }
    }

    /** Aplica la respuesta del servidor. Devuelve el cambio de guía si el servidor tuvo que asignar otra. */
    private suspend fun aplicarExito(operacion: OperacionPendienteEntity, idLocal: Int, remoto: EnvioDto?): GuiaCambiada? =
        db.withTransaction {
            operacionDao.eliminar(operacion)
            val actual = envioDao.porId(idLocal) ?: return@withTransaction null
            if (remoto == null) {
                envioDao.eliminar(actual)
                return@withTransaction null
            }
            val cambio = cambioDeGuia(operacion.tipoOperacion, actual.numeroGuia, remoto.numeroGuia)
            val guiaAnterior = cambio?.anterior ?: actual.guiaAnterior
            if (operacionDao.deEntidad(idLocal).isNotEmpty()) {
                // Quedan más cambios de este envío en la cola: se guarda el identificador
                // y la versión del servidor, pero sigue pendiente.
                envioDao.actualizar(
                    actual.copy(
                        idRemoto = remoto.id,
                        numeroGuia = remoto.numeroGuia,
                        version = remoto.version,
                        estadoSync = EstadoSincronizacion.PENDIENTE.name,
                        mensajeError = null,
                        guiaAnterior = guiaAnterior
                    )
                )
            } else {
                envioDao.actualizar(remoto.aEntidad(idLocal).copy(guiaAnterior = guiaAnterior))
            }
            cambio
        }

    /** RF11: actualiza Room con lo descargado, sin sobrescribir cambios locales pendientes. */
    /** Guarda lo descargado y devuelve los envíos que cambiaron de estado. */
    private suspend fun guardarDescarga(remotos: List<EnvioDto>, horaServidor: String?): List<CambioEstado> =
        db.withTransaction {
            val cambios = mutableListOf<CambioEstado>()
            val locales = envioDao.todos()
            val porUuid = locales.associateBy { it.uuid }
            val conCambiosLocales = operacionDao.todas().map { it.idEntidadLocal }.toSet()
            val sincronizado = EstadoSincronizacion.SINCRONIZADO.name

            // Del más antiguo al más nuevo, para que el orden local coincida con el del servidor.
            remotos.sortedBy { it.id }.forEach { remoto ->
                val local = porUuid[remoto.uuid]
                when {
                    local == null -> envioDao.insertar(remoto.aEntidad())
                    local.id in conCambiosLocales || local.estadoSync != sincronizado -> Unit
                    else -> {
                        if (local.estado != remoto.estado) cambios += CambioEstado(remoto.numeroGuia, EstadoEnvio.desde(remoto.estado))
                        // La guía anterior es solo del celular: el servidor no la conoce y no debe perderse.
                        envioDao.actualizar(remoto.aEntidad(local.id).copy(guiaAnterior = local.guiaAnterior))
                    }
                }
            }

            // Lo que el servidor ya no envía (se eliminó allá) se quita del teléfono.
            val uuidsRemotos = remotos.map { it.uuid }.toSet()
            locales
                .filter {
                    it.idRemoto != null && it.uuid !in uuidsRemotos &&
                        it.id !in conCambiosLocales && it.estadoSync == sincronizado
                }
                .forEach { envioDao.eliminar(it) }

            operacionDao.guardarMetadata(
                SyncMetadataEntity(RECURSO_ENVIOS, System.currentTimeMillis(), horaServidor)
            )
            cambios
        }

    private companion object {
        const val RECURSO_ENVIOS = "envios"
    }
}
