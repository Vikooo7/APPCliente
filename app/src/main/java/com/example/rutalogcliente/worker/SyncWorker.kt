package com.example.rutalogcliente.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.rutalogcliente.RutaLogApplication
import com.example.rutalogcliente.util.Notificaciones
import java.util.concurrent.TimeUnit

/**
 * RF13: sincroniza en segundo plano con WorkManager. Solo se ejecuta cuando hay red, así que
 * una operación guardada sin conexión se envía sola al recuperarla, aunque la pantalla de
 * sincronización no esté abierta.
 */
class SyncWorker(context: Context, parametros: WorkerParameters) : CoroutineWorker(context, parametros) {

    override suspend fun doWork(): Result {
        val contenedor = (applicationContext as RutaLogApplication).contenedor
        if (contenedor.sesion.token == null) return Result.success()

        val resultado = contenedor.syncRepository.sincronizar()
        // RF-R05: aviso con el número de operación cuando Banco Andino transfiere una liquidación.
        if (resultado.error == null) {
            Notificaciones.avisarLiquidaciones(applicationContext, contenedor.pagoRepository.liquidacionesPorAvisar())
        }
        return if (resultado.error != null && runAttemptCount < MAX_INTENTOS) Result.retry() else Result.success()
    }

    companion object {
        private const val NOMBRE = "sincronizacion_rutalog"
        private const val NOMBRE_PERIODICA = "sincronizacion_rutalog_periodica"
        private const val MAX_INTENTOS = 5

        /**
         * Cada 15 minutos (el mínimo de Android) revisa si los envíos avanzaron, para avisar al remitente
         * aunque no abra la app. Solo corre con red y, sin sesión, termina sin hacer nada.
         */
        fun programarPeriodica(context: Context) {
            val trabajo = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NOMBRE_PERIODICA, ExistingPeriodicWorkPolicy.KEEP, trabajo)
        }

        /** Programa una sincronización para cuando haya conexión. */
        fun programar(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(NOMBRE, ExistingWorkPolicy.REPLACE, trabajo)
        }
    }
}
