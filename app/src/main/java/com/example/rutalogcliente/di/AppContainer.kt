package com.example.rutalogcliente.di

import android.content.Context
import com.example.rutalogcliente.data.local.AppDatabase
import com.example.rutalogcliente.data.local.SesionSegura
import com.example.rutalogcliente.data.remote.RetrofitClient
import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.repository.AuthRepository
import com.example.rutalogcliente.data.repository.EnvioRepository
import com.example.rutalogcliente.data.repository.PagoRepository
import com.example.rutalogcliente.data.repository.SeguimientoRepository
import com.example.rutalogcliente.data.repository.SyncRepository
import com.example.rutalogcliente.util.ConnectivityObserver
import com.example.rutalogcliente.util.Notificaciones
import com.example.rutalogcliente.worker.SyncWorker
import java.util.concurrent.atomic.AtomicBoolean
import com.example.rutalogcliente.data.repository.PushRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Crea una sola vez las piezas que comparten los ViewModel y el SyncWorker:
 * la base de datos Room, el cliente de la API y los repositorios.
 */
class AppContainer(context: Context) {

    private val contextoApp = context.applicationContext

    val db: AppDatabase = AppDatabase.getDB(contextoApp)
    val sesion = SesionSegura(contextoApp)
    val conectividad = ConnectivityObserver(contextoApp)

    /** Prueba de la exposición: mientras esté en true, la API responde con error 503. */
    val simularErrorServidor = AtomicBoolean(false)

    val api: ApiService = RetrofitClient.crear(
        token = { sesion.token },
        simularError = { simularErrorServidor.get() }
    )

    /** Tareas cortas que no dependen de una pantalla (registrar el celular para las notificaciones push). */
    private val ambito = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val pushRepository = PushRepository(api)

    val authRepository = AuthRepository(api, db, sesion) { iniciada, tokenAnterior ->
        if (iniciada) registrarPush() else tokenAnterior?.let { t -> ambito.launch { pushRepository.olvidar(t) } }
    }

    /** Registra este celular para recibir los avisos en tiempo real (al abrir la app y al iniciar sesión). */
    fun registrarPush() {
        if (sesion.token != null) ambito.launch { pushRepository.registrar() }
    }
    val envioRepository = EnvioRepository(db)
    val syncRepository = SyncRepository(
        db = db,
        api = api,
        alCambiarEstados = { cambios -> Notificaciones.avisarCambios(contextoApp, cambios) },
        alCambiarGuias = { cambios -> Notificaciones.avisarGuiasCambiadas(contextoApp, cambios) }
    )
    val seguimientoRepository = SeguimientoRepository(api)
    val pagoRepository = PagoRepository(api, contextoApp)

    fun programarSincronizacion() {
        SyncWorker.programar(contextoApp)
    }
}
