package com.example.rutalogcliente

import android.app.Application
import com.example.rutalogcliente.di.AppContainer
import com.example.rutalogcliente.util.Notificaciones
import com.example.rutalogcliente.worker.SyncWorker

/** Clase Application: crea el contenedor de dependencias al iniciar la app. */
class RutaLogApplication : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
        // Avisos de estado de los envíos: canal de notificaciones y revisión cada 15 minutos.
        Notificaciones.crearCanal(this)
        SyncWorker.programarPeriodica(this)
    }
}
