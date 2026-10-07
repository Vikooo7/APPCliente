package com.example.rutalogcliente

import android.app.Application
import com.example.rutalogcliente.di.AppContainer

/** Clase Application: crea el contenedor de dependencias al iniciar la app. */
class RutaLogApplication : Application() {

    lateinit var contenedor: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = AppContainer(this)
    }
}
