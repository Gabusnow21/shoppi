package com.shoppi

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.shoppi.data.remote.dto.FcmTokenRequest
import com.shoppi.config.RemoteConfigManager
import com.shoppi.util.SessionManager
import kotlinx.coroutines.tasks.await

/**
 * Gestión del token FCM: registro en el backend y borrado en logout.
 */
object FcmTokenManager {

    /** Pide el permiso de notificaciones en Android 13+. Devuelve true si ya estaba concedido. */
    fun tienePermisoNotificaciones(contexto: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                contexto,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** Flag gobernado por Remote Config (T-250). */
    fun estaHabilitado(contexto: Context): Boolean = RemoteConfigManager.fcmEnabled

    fun setHabilitado(contexto: Context, valor: Boolean) =
        contexto.getSharedPreferences("shoppi_fcm", Context.MODE_PRIVATE)
            .edit().putBoolean("fcm_enabled", valor).apply()

    /** Obtiene el token FCM actual y lo registra en el backend. */
    suspend fun registrarToken(contexto: Context, token: String? = null) {
        if (!estaHabilitado(contexto)) return
        val session = (contexto.applicationContext as? ShoppiApp)?.container?.session
            ?: SessionManager(contexto)
        if (session.token().isNullOrBlank()) return

        val fcmToken = token ?: runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (fcmToken.isNullOrBlank()) return

        val api = (contexto.applicationContext as? ShoppiApp)?.container?.api
        runCatching {
            api?.registrarFcmToken(
                FcmTokenRequest(token = fcmToken, deviceId = android.os.Build.MODEL, plataforma = "ANDROID")
            )
        }
    }

    /** Borra el token del backend y del dispositivo (logout). */
    suspend fun borrarToken(contexto: Context) {
        val session = (contexto.applicationContext as? ShoppiApp)?.container?.session
            ?: SessionManager(contexto)
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        if (!token.isNullOrBlank() && !session.token().isNullOrBlank()) {
            val api = (contexto.applicationContext as? ShoppiApp)?.container?.api
            runCatching { api?.borrarFcmToken(FcmTokenRequest(token = token)) }
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
    }
}
