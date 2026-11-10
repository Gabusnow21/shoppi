package com.shoppi.config

import android.content.Context
import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.shoppi.BuildConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lectura de Remote Config (Firebase auxiliar).
 *
 * Defaults seguros para que la app funcione offline/sin publicar config.
 * Nunca decide precios, stock ni totales: solo config/UX.
 */
object RemoteConfigManager {

    private const val TAG = "RemoteConfig"

    private lateinit var remoteConfig: FirebaseRemoteConfig

    private val _syncEnabled = MutableStateFlow(true)
    val syncEnabledFlow: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _maintenanceMode = MutableStateFlow(false)
    val maintenanceModeFlow: StateFlow<Boolean> = _maintenanceMode.asStateFlow()

    fun inicializar(context: Context) {
        remoteConfig = FirebaseRemoteConfig.getInstance()
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 12L * 3600L)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)

        val defaults = mapOf(
            "api_base_url" to "http://10.0.2.2:8080/api/",
            "sync_enabled" to true,
            "maintenance_mode" to false,
            "fcm_enabled" to true,
            "storage_max_mb" to 5L,
        )
        runBlocking { remoteConfig.setDefaultsAsync(defaults).await() }
        actualizarEstado()
    }

    suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            Log.w(TAG, "fetchAndActivate falló, se usan defaults/caché", e)
        }
        actualizarEstado()
    }

    private fun actualizarEstado() {
        _syncEnabled.value = remoteConfig.getBoolean("sync_enabled")
        _maintenanceMode.value = remoteConfig.getBoolean("maintenance_mode")
    }

    val apiBaseUrl: String
        get() = remoteConfig.getString("api_base_url").ifBlank { BuildConfig.API_BASE_URL }

    val syncEnabled: Boolean
        get() = remoteConfig.getBoolean("sync_enabled")

    val maintenanceMode: Boolean
        get() = remoteConfig.getBoolean("maintenance_mode")

    val fcmEnabled: Boolean
        get() = remoteConfig.getBoolean("fcm_enabled")

    val storageMaxMb: Int
        get() = remoteConfig.getLong("storage_max_mb").toInt().coerceAtLeast(1)
}