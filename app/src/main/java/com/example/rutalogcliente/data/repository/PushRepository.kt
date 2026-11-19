package com.example.rutalogcliente.data.repository

import com.example.rutalogcliente.data.remote.ResultadoApi
import com.example.rutalogcliente.data.remote.api.ApiService
import com.example.rutalogcliente.data.remote.dto.DispositivoRequest
import com.example.rutalogcliente.data.remote.llamarApi
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Notificaciones push en tiempo real: registra el token de Firebase Cloud Messaging de este celular en la API,
 * para que el servidor avise al instante cuando el courier recoge, saca a reparto, entrega o no puede entregar.
 * Si falla (sin señal), no pasa nada: se vuelve a intentar al abrir la app y los avisos igual llegan al sincronizar.
 */
class PushRepository(private val api: ApiService) {

    /** Token actual del celular, o null si Google Play services no lo puede dar ahora. */
    private suspend fun tokenDelCelular(): String? =
        runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()

    suspend fun registrar(): Boolean {
        val token = tokenDelCelular() ?: return false
        return llamarApi { api.registrarDispositivo(DispositivoRequest(token)) } is ResultadoApi.Exito
    }

    /** Al cerrar sesión, con el token de esa sesión (la app ya lo borró de su almacenamiento). */
    suspend fun olvidar(tokenSesion: String) {
        val token = tokenDelCelular() ?: return
        llamarApi { api.borrarDispositivo("Bearer $tokenSesion", DispositivoRequest(token)) }
    }
}
