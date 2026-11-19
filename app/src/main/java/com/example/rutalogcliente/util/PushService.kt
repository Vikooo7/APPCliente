package com.example.rutalogcliente.util

import com.example.rutalogcliente.RutaLogApplication
import com.example.rutalogcliente.worker.SyncWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe las notificaciones push de RutaLog (Firebase Cloud Messaging).
 * - Con la app cerrada o en segundo plano, Android muestra la notificación solo (la API manda título y texto).
 * - Con la app abierta llega aquí: se muestra igual y se sincroniza para que la pantalla tenga el estado nuevo.
 */
class PushService : FirebaseMessagingService() {

    /** Google cambió el token de este celular: se vuelve a registrar en la API. */
    override fun onNewToken(token: String) {
        (application as RutaLogApplication).contenedor.registrarPush()
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val datos = mensaje.data
        val titulo = datos["titulo"] ?: mensaje.notification?.title ?: return
        val cuerpo = datos["cuerpo"] ?: mensaje.notification?.body.orEmpty()
        Notificaciones.mostrarPush(this, titulo, cuerpo, datos["guia"])
        SyncWorker.programar(this)
    }
}
