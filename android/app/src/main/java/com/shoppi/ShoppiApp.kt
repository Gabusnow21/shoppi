package com.shoppi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Punto de entrada de la app.
 *
 * Construye el [AppContainer] y programa la sincronización periódica de
 * WorkManager (cada 15 minutos, solo con red). Crea los canales de notificación
 * para FCM.
 */
class ShoppiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.shoppi.config.RemoteConfigManager.inicializar(this)
        container = AppContainer(this)
        container.programarSyncPeriodico()
        crearCanalesNotificacion()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.shoppi.config.RemoteConfigManager.fetchAndActivate()
        }
    }

    private fun crearCanalesNotificacion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canales = listOf(
            NotificationChannel(ShoppiFirebaseService.CHANNEL_VENTAS, "Ventas", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(ShoppiFirebaseService.CHANNEL_STOCK, "Stock", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(ShoppiFirebaseService.CHANNEL_SYNC, "Sincronización", NotificationManager.IMPORTANCE_LOW)
        )
        canales.forEach { manager.createNotificationChannel(it) }
    }
}
