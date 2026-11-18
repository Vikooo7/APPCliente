package com.example.rutalogcliente.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

/**
 * Informa si el teléfono tiene conexión a internet y avisa cuando cambia.
 *
 * Con la pantalla apagada o la app en segundo plano (por ejemplo, mientras se paga en Banco Andino),
 * Android bloquea la red de la app para ahorrar batería y avisa como si se hubiera perdido. Al volver,
 * no llega un "red disponible" sino un "ya no está bloqueada" (onBlockedStatusChanged): si no se
 * escucha, la app se queda creyendo que no hay conexión y no sincroniza. Por eso también se vuelve a
 * revisar cada vez que la app pasa al frente ([revisarAhora], desde MainActivity.onResume).
 */
class ConnectivityObserver(context: Context) {

    private val administrador = context.getSystemService(ConnectivityManager::class.java)

    /** Pedidos de volver a revisar la conexión (al volver la app al frente). */
    private val revisiones = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val enLinea: Flow<Boolean> = merge(
        callbackFlow {
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

                // Ahorro de batería: la red sigue ahí, pero bloqueada para esta app mientras está en segundo plano.
                override fun onBlockedStatusChanged(network: Network, bloqueada: Boolean) {
                    trySend(!bloqueada && hayConexion())
                }
            }
            trySend(hayConexion())
            administrador.registerDefaultNetworkCallback(escucha)
            awaitClose { administrador.unregisterNetworkCallback(escucha) }
        },
        revisiones.map { hayConexion() }
    ).distinctUntilChanged()

    /** Vuelve a mirar la red ahora mismo (al encender la pantalla o volver de otra app). */
    fun revisarAhora() {
        revisiones.tryEmit(Unit)
    }

    fun hayConexion(): Boolean {
        val red = administrador.activeNetwork ?: return false
        val capacidades = administrador.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
