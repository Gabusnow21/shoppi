package com.shoppi

import android.app.Application
import com.shoppi.data.remote.RetrofitClient

/**
 * Clase Application de Shoppi.
 *
 * Punto único de inicialización de la app. En tareas posteriores se inicializan aquí:
 * - Room / ShoppiDB (T-110)
 * - Firebase (FASE 3B) y WorkManager (sync)
 */
class ShoppiApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Cliente Retrofit + sesión JWT compartida (T-103).
        RetrofitClient.init(this)
    }
}
