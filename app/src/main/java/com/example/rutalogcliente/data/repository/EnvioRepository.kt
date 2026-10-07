package com.example.rutalogcliente.data.repository

import androidx.room.withTransaction
import com.example.rutalogcliente.data.local.AppDatabase
import com.example.rutalogcliente.data.local.entities.EnvioEntity
import com.example.rutalogcliente.data.local.entities.OperacionPendienteEntity
import com.example.rutalogcliente.data.local.entities.TipoOperacion
import com.example.rutalogcliente.data.mapper.aModelo
import com.example.rutalogcliente.model.CatalogoRutas
import com.example.rutalogcliente.model.Envio
import com.example.rutalogcliente.model.EstadoEnvio
import com.example.rutalogcliente.model.EstadoSincronizacion
import com.example.rutalogcliente.model.NumeroGuia
import com.example.rutalogcliente.model.RutaTarifa
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Datos de una operación tal como se guardan en el campo payload (JSON). */
data class PayloadEnvio(
    val uuid: String,
    val numeroGuia: String,
    val ruta: String,
    val pesoKg: Double
)

/**
 * Envíos del cliente. Las pantallas siempre leen de Room; cada escritura se guarda en Room
 * junto con su operación pendiente, en una sola transacción (RF12). El envío a la API lo
 * hace después SyncRepository, haya o no conexión en este momento.
 */
class EnvioRepository(private val db: AppDatabase) {

    private val envioDao = db.envioDao()
    private val operacionDao = db.operacionPendienteDao()
    private val gson = Gson()

    fun observarEnvios(): Flow<List<Envio>> =
        envioDao.obtenerTodos().map { lista -> lista.map { it.aModelo() } }

    fun observarPorId(id: Int): Flow<Envio?> =
        envioDao.observarPorId(id).map { entidad -> entidad?.takeIf { !it.eliminadoLocal }?.aModelo() }

    /** RF09: busca en los datos locales, así funciona también sin conexión. */
    suspend fun buscarPorGuia(guiaNormalizada: String): Envio? =
        envioDao.buscarPorGuia(guiaNormalizada)?.aModelo()

    /** RF06 + RF07 + RF08: guarda el envío con su guía y su costo, y lo deja en la cola. */
    suspend fun registrar(ruta: RutaTarifa, pesoKg: Double): Envio = db.withTransaction {
        val ultimo = envioDao.guias()
            .mapNotNull { NumeroGuia.correlativo(it) }
            .filter { it < NumeroGuia.INICIO_SERVIDOR }
            .maxOrNull()
        val correlativo = if (ultimo == null) NumeroGuia.PRIMER_CORRELATIVO else ultimo + 1

        val nuevo = EnvioEntity(
            idRemoto = null,
            uuid = UUID.randomUUID().toString(),
            numeroGuia = NumeroGuia.generar(correlativo),
            ruta = ruta.nombre,
            pesoKg = pesoKg,
            costoEnvio = CatalogoRutas.calcularCosto(pesoKg, ruta.tarifaPorKg),
            estado = EstadoEnvio.PENDIENTE.codigo,
            transportistaAsignado = null,
            version = 1,
            estadoSync = EstadoSincronizacion.PENDIENTE.name
        )
        val guardado = nuevo.copy(id = envioDao.insertar(nuevo).toInt())
        encolar(TipoOperacion.CREAR, guardado)
        guardado.aModelo()
    }

    /** Cambia la ruta o el peso de un envío y deja el cambio en la cola. */
    suspend fun actualizar(idLocal: Int, ruta: RutaTarifa, pesoKg: Double) {
        db.withTransaction {
            val actual = envioDao.porId(idLocal) ?: return@withTransaction
            val cambiado = actual.copy(
                ruta = ruta.nombre,
                pesoKg = pesoKg,
                costoEnvio = CatalogoRutas.calcularCosto(pesoKg, ruta.tarifaPorKg),
                estadoSync = EstadoSincronizacion.PENDIENTE.name,
                mensajeError = null
            )
            envioDao.actualizar(cambiado)
            encolar(TipoOperacion.ACTUALIZAR, cambiado)
        }
    }

    /** Elimina un envío. Si el servidor ya lo conoce, la eliminación queda en la cola. */
    suspend fun eliminar(idLocal: Int) {
        db.withTransaction {
            val actual = envioDao.porId(idLocal) ?: return@withTransaction
            // Los cambios que estaban en cola para este envío ya no tienen sentido.
            operacionDao.eliminarDeEntidad(idLocal)
            if (actual.idRemoto == null) {
                // Nunca llegó al servidor: basta con borrarlo del teléfono.
                envioDao.eliminar(actual)
            } else {
                envioDao.actualizar(
                    actual.copy(
                        eliminadoLocal = true,
                        estadoSync = EstadoSincronizacion.PENDIENTE.name,
                        mensajeError = null
                    )
                )
                operacionDao.insertar(nuevaOperacion(TipoOperacion.ELIMINAR, actual))
            }
        }
    }

    /**
     * Agrega la operación a la cola. Si ya hay una de crear o actualizar para el mismo envío
     * que aún no se envió (o que el servidor rechazó), se reutiliza con los datos nuevos:
     * así el servidor recibe una sola operación con el estado final.
     */
    private suspend fun encolar(tipo: String, envio: EnvioEntity) {
        val reutilizable = operacionDao.deEntidad(envio.id).lastOrNull { operacion ->
            operacion.tipoOperacion != TipoOperacion.ELIMINAR &&
                operacion.estado != EstadoSincronizacion.ENVIANDO.name &&
                (operacion.intentos == 0 || operacion.estado == EstadoSincronizacion.ERROR.name)
        }
        if (reutilizable != null) {
            operacionDao.actualizar(
                reutilizable.copy(
                    payload = payload(envio),
                    estado = EstadoSincronizacion.PENDIENTE.name,
                    mensajeError = null
                )
            )
        } else {
            operacionDao.insertar(nuevaOperacion(tipo, envio))
        }
    }

    private fun nuevaOperacion(tipo: String, envio: EnvioEntity) = OperacionPendienteEntity(
        uuidOperacion = UUID.randomUUID().toString(),
        entidad = "envios",
        idEntidadLocal = envio.id,
        tipoOperacion = tipo,
        payload = payload(envio),
        fechaRegistro = System.currentTimeMillis(),
        estado = EstadoSincronizacion.PENDIENTE.name
    )

    private fun payload(envio: EnvioEntity): String =
        gson.toJson(PayloadEnvio(envio.uuid, envio.numeroGuia, envio.ruta, envio.pesoKg))
}
