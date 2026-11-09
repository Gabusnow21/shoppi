package com.shoppi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch
import com.shoppi.sync.SyncManager

/**
 * Servicio FCM de Shoppi.
 *
 * - `onNewToken`: re-registra el token en el backend.
 * - `onMessageReceived`: muestra la notificación en el canal correspondiente y,
 *   si el payload trae `type=SYNC_PULL`, dispara una sincronización.
 */
class ShoppiFirebaseService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            FcmTokenManager.registrarToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"]

        if (type == "SYNC_PULL") {
            (application as? ShoppiApp)?.container?.syncManager?.requestPull()
        }

        crearCanales()

        val channelId = when (type) {
            "VENTA_CONFIRMADA" -> CHANNEL_VENTAS
            "STOCK_BAJO" -> CHANNEL_STOCK
            else -> CHANNEL_SYNC
        }

        val titulo = message.notification?.title ?: data["title"] ?: "Shoppi"
        val cuerpo = message.notification?.body ?: data["body"] ?: ""

        val intent = android.content.Intent(this, MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("push_type", type)
            data["ventaId"]?.let { putExtra("ventaId", it.toLongOrNull() ?: -1L) }
            data["productoId"]?.let { putExtra("productoId", it.toLongOrNull() ?: -1L) }
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), notificacion)
    }

    private fun crearCanales() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val canales = listOf(
            NotificationChannel(CHANNEL_VENTAS, "Ventas", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_STOCK, "Stock", NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel(CHANNEL_SYNC, "Sincronización", NotificationManager.IMPORTANCE_LOW)
        )
        canales.forEach { manager.createNotificationChannel(it) }
    }

    companion object {
        const val CHANNEL_VENTAS = "ventas"
        const val CHANNEL_STOCK = "stock"
        const val CHANNEL_SYNC = "sync"
    }
}
