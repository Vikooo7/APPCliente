package com.example.rutalogcliente.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Informa si el teléfono tiene conexión a internet y avisa cuando cambia. */
class ConnectivityObserver(context: Context) {

    private val administrador = context.getSystemService(ConnectivityManager::class.java)

    val enLinea: Flow<Boolean> = callbackFlow {
        val escucha = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(hayConexion())
            }

            // Si otra red toma el relevo (por ejemplo, datos móviles), llega después un onAvailable.
            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, capacidades: NetworkCapabilities) {
                trySend(hayConexion())
            }
        }
        trySend(hayConexion())
        administrador.registerDefaultNetworkCallback(escucha)
        awaitClose { administrador.unregisterNetworkCallback(escucha) }
    }.distinctUntilChanged()

    fun hayConexion(): Boolean {
        val red = administrador.activeNetwork ?: return false
        val capacidades = administrador.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
