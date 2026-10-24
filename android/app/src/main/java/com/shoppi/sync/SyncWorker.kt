package com.shoppi.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.shoppi.ShoppiApp

/**
 * Worker que ejecuta [SyncManager.sincronizarTodo] en segundo plano.
 *
 * Reintenta con backoff si se cortó por falta de red. Los registros `FAILED`
 * requieren corrección del usuario (dato inválido, sesión vencida), así que no
 * se reintentan en bucle: el próximo cambio o la sincronización manual los
 * vuelven a intentar.
 */
class SyncWorker(
    appContext: Context,
    parametros: WorkerParameters
) : CoroutineWorker(appContext, parametros) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ShoppiApp ?: return Result.failure()

        val resultado = app.container.syncManager.sincronizarTodo()

        return if (resultado.sinConexion) Result.retry() else Result.success()
    }
}
