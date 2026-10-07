package com.example.rutalogcliente.di

import android.content.Context
import com.example.rutalogcliente.data.local.AppDatabase
import com.example.rutalogcliente.data.local.SesionSegura
import com.example.rutalogcliente.data.remote.RetrofitClient
import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.repository.AuthRepository
import com.example.rutalogcliente.data.repository.EnvioRepository
import com.example.rutalogcliente.data.repository.SyncRepository
import com.example.rutalogcliente.util.ConnectivityObserver
import com.example.rutalogcliente.worker.SyncWorker
import java.util.concurrent.atomic.AtomicBoolean

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

    val authRepository = AuthRepository(api, db, sesion)
    val envioRepository = EnvioRepository(db)
    val syncRepository = SyncRepository(db, api)

    fun programarSincronizacion() {
        SyncWorker.programar(contextoApp)
    }
}
