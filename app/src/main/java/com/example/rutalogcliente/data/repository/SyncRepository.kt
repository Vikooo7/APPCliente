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
    val error: String? = null
)

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
    private val api: ApiService
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

        for (operacion in operacionDao.porEnviar()) {
            val envio = envioDao.porId(operacion.idEntidadLocal)
            if (envio == null) {
                operacionDao.eliminar(operacion)
                continue
            }

            marcar(operacion, envio, EstadoSincronizacion.ENVIANDO)

            when (val resultado = enviar(operacion, envio)) {
                is ResultadoApi.Exito -> {
                    aplicarExito(operacion, envio.id, resultado.valor)
                    enviadas++
                }

                is ResultadoApi.Rechazo -> {
                    if (resultado.http == 401) {
                        marcar(operacion, envio, EstadoSincronizacion.PENDIENTE)
                        return@withLock ResultadoSync(enviadas, rechazadas, error = resultado.mensaje)
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

        when (val descarga = llamarApi { api.obtenerEnvios() }) {
            is ResultadoApi.Exito -> {
                guardarDescarga(descarga.valor.datos, descarga.valor.servidorAhora)
                ResultadoSync(enviadas, rechazadas, descargados = descarga.valor.datos.size)
            }

            is ResultadoApi.Rechazo -> ResultadoSync(enviadas, rechazadas, error = descarga.mensaje)
            is ResultadoApi.Fallo -> ResultadoSync(enviadas, rechazadas, error = descarga.mensaje)
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
                    CrearEnvioRequest(operacion.uuidOperacion, envio.uuid, datos.numeroGuia, datos.ruta, datos.pesoKg)
                )
            }

            TipoOperacion.ACTUALIZAR -> {
                val idRemoto = envio.idRemoto
                    ?: return ResultadoApi.Rechazo(0, "SIN_REGISTRO", "Primero debe aceptarse el registro de este envío.")
                llamarApi {
                    api.actualizarEnvio(
                        idRemoto,
                        ActualizarEnvioRequest(operacion.uuidOperacion, datos.ruta, datos.pesoKg, envio.version)
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

    private suspend fun aplicarExito(operacion: OperacionPendienteEntity, idLocal: Int, remoto: EnvioDto?) {
        db.withTransaction {
            operacionDao.eliminar(operacion)
            val actual = envioDao.porId(idLocal) ?: return@withTransaction
            when {
                remoto == null -> envioDao.eliminar(actual)

                // Quedan más cambios de este envío en la cola: se guarda el identificador
                // y la versión del servidor, pero sigue pendiente.
                operacionDao.deEntidad(idLocal).isNotEmpty() -> envioDao.actualizar(
                    actual.copy(
                        idRemoto = remoto.id,
                        numeroGuia = remoto.numeroGuia,
                        version = remoto.version,
                        estadoSync = EstadoSincronizacion.PENDIENTE.name,
                        mensajeError = null
                    )
                )

                else -> envioDao.actualizar(remoto.aEntidad(idLocal))
            }
        }
    }

    /** RF11: actualiza Room con lo descargado, sin sobrescribir cambios locales pendientes. */
    private suspend fun guardarDescarga(remotos: List<EnvioDto>, horaServidor: String?) {
        db.withTransaction {
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
                    else -> envioDao.actualizar(remoto.aEntidad(local.id))
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
        }
    }

    private companion object {
        const val RECURSO_ENVIOS = "envios"
    }
}
