package com.shoppi.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Programación de la sincronización con WorkManager.
 *
 * - Periódica cada 15 minutos (mínimo permitido) mientras haya red.
 * - One-shot inmediato tras cada mutación del usuario.
 *
 * `KEEP` evita encolar decenas de workers si el usuario guarda rápido varias
 * veces; el que ya está corriendo tomará los cambios más recientes en su pull.
 */
object SyncScheduler {

    private const val TRABAJO_PERIODICO = "shoppi-sync-periodico"
    private const val TRABAJO_INMEDIATO = "shoppi-sync-inmediato"

    fun programarPeriodico(contexto: Context) {
        val solicitud = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(restricciones())
            .build()

        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
            TRABAJO_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            solicitud
        )
    }

    fun sincronizarAhora(contexto: Context) {
        val solicitud = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(restricciones())
            .build()

        WorkManager.getInstance(contexto).enqueueUniqueWork(
            TRABAJO_INMEDIATO,
            ExistingWorkPolicy.KEEP,
            solicitud
        )
    }

    private fun restricciones(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
