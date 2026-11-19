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

    /**
     * Canal de importancia alta: el aviso aparece en la parte de arriba de la pantalla, en tiempo real.
     * Es un canal nuevo porque Android no deja subir la importancia de uno que ya existe ("estado_envios").
     * El mismo id lo usa la API en cada push (src/push.js).
     */
    private const val CANAL = "envios_tiempo_real"
    private const val CANAL_ANTERIOR = "estado_envios"

    fun crearCanal(context: Context) {
        val canal = NotificationChannel(CANAL, "Estado de mis envíos", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisa al instante cuando el courier recoge tu envío, sale a reparto, lo entrega o no puede entregarlo."
        }
        val gestor = context.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(canal)
        gestor.deleteNotificationChannel(CANAL_ANTERIOR)
    }

    fun avisarCambios(context: Context, cambios: List<CambioEstado>) {
        if (cambios.isEmpty()) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return // sin permiso, los cambios igual se ven en la app
        }
        val gestor = NotificationManagerCompat.from(context)
        // Una notificación por guía (etiqueta = guía): si el mismo envío vuelve a avanzar, o si ya llegó el push
        // en tiempo real con la misma etiqueta, se reemplaza en vez de duplicarse.
        cambios.forEach { cambio ->
            val aviso = NotificationCompat.Builder(context, CANAL)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("${cambio.numeroGuia} · ${titulo(cambio.estado)}")
                .setContentText(cambio.estado.descripcion)
                .setStyle(NotificationCompat.BigTextStyle().bigText(cambio.estado.descripcion))
                .setContentIntent(abrirEnvio(context, cambio.numeroGuia))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .build()
            try {
                gestor.notify(cambio.numeroGuia, 0, aviso)
            } catch (e: SecurityException) {
                // El usuario quitó el permiso justo ahora: no se avisa.
            }
        }
    }

    /** Push en tiempo real recibido con la app abierta (con la app cerrada lo muestra Android, con la misma etiqueta). */
    fun mostrarPush(context: Context, titulo: String, cuerpo: String, guia: String?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val aviso = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(cuerpo))
            .setContentIntent(abrirEnvio(context, guia))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(guia ?: "rutalog", 0, aviso)
        } catch (e: SecurityException) {
            // permiso retirado: el cambio igual se ve en la app
        }
    }

    /** Al tocar la notificación se abre el seguimiento de esa guía (si la hay). */
    private fun abrirEnvio(context: Context, guia: String?): PendingIntent = PendingIntent.getActivity(
        context,
        guia?.hashCode() ?: 0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { guia?.let { putExtra(EXTRA_GUIA, it) } },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** Mismo nombre que el dato "guia" del push: Android lo pone en el Intent al tocar una notificación del sistema. */
    const val EXTRA_GUIA = "guia"

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
