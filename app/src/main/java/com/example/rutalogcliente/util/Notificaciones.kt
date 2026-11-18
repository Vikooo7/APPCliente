package com.example.rutalogcliente.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.rutalogcliente.MainActivity
import com.example.rutalogcliente.data.remote.dto.LiquidacionDto
import com.example.rutalogcliente.data.repository.CambioEstado
import com.example.rutalogcliente.data.repository.GuiaCambiada
import com.example.rutalogcliente.model.EstadoEnvio

/**
 * Avisos al remitente cuando su envío avanza (recogido, en reparto, entregado, entrega fallida...).
 * Los cambios llegan al sincronizar: al abrir la app, al recuperar la señal o cada 15 minutos.
 */
object Notificaciones {

    private const val CANAL = "estado_envios"

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Estado de mis envíos", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Avisa cuando tu envío es recogido, sale a reparto, se entrega o no se pudo entregar."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun avisarCambios(context: Context, cambios: List<CambioEstado>) {
        if (cambios.isEmpty()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return // sin permiso, los cambios igual se ven en la app
        }
        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val gestor = NotificationManagerCompat.from(context)
        // Una notificación por guía: si el mismo envío vuelve a avanzar, se reemplaza la anterior.
        cambios.forEach { cambio ->
            val aviso = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("${cambio.numeroGuia} · ${titulo(cambio.estado)}")
                .setContentText(cambio.estado.descripcion)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cambio.estado.descripcion))
                .setContentIntent(abrirApp)
                .setAutoCancel(true)
                .build()
            try {
                gestor.notify(cambio.numeroGuia.hashCode(), aviso)
            } catch (e: SecurityException) {
                // El usuario quitó el permiso justo ahora: no se avisa.
            }
        }
    }

    /** La guía que generó el celular ya existía: el servidor asignó otra. Se avisa para no usar la vieja. */
    fun avisarGuiasCambiadas(context: Context, cambios: List<GuiaCambiada>) {
        if (cambios.isEmpty()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val gestor = NotificationManagerCompat.from(context)
        cambios.forEach { c ->
            val texto = "Tu envío ${c.anterior} ahora es ${c.nueva}: ese número ya existía. " +
                "Usa la guía nueva para el paquete y el QR."
            val aviso = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Nueva guía: ${c.nueva}")
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .build()
            try {
                gestor.notify(c.nueva.hashCode() + 1, aviso)
            } catch (e: SecurityException) {
                // permiso retirado: el cambio igual se ve en el detalle del envío
            }
        }
    }

    /** RF-R05: "recibo una notificación con el número de operación del banco". */
    fun avisarLiquidaciones(context: Context, liquidaciones: List<LiquidacionDto>) {
        if (liquidaciones.isEmpty()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val gestor = NotificationManagerCompat.from(context)
        liquidaciones.forEach { l ->
            val texto = "Banco Andino transfirió S/ ${"%,.2f".format(l.neto)} a tu cuenta · operación ${l.nroOperacion}"
            val aviso = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Liquidación recibida")
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setAutoCancel(true)
                .build()
            try {
                gestor.notify(100_000 + l.id, aviso)
            } catch (e: SecurityException) {
                // permiso retirado: no se avisa
            }
        }
    }

    private fun titulo(estado: EstadoEnvio): String = when (estado) {
        EstadoEnvio.RECOGIDO -> "El courier recogió tu envío"
        EstadoEnvio.EN_TRANSITO -> "Tu envío va en camino"
        EstadoEnvio.EN_REPARTO -> "Tu envío salió a reparto"
        EstadoEnvio.ENTREGADO -> "Envío entregado"
        EstadoEnvio.FALLIDO -> "No se pudo entregar"
        EstadoEnvio.REPROGRAMADO -> "Entrega reprogramada"
        EstadoEnvio.DEVUELTO -> "Tu envío vuelve a ti"
        EstadoEnvio.PENDIENTE -> "Pendiente de recojo"
    }
}
