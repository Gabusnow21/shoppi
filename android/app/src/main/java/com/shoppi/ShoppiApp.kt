package com.shoppi

import android.app.Application

/**
 * Clase Application de Shoppi.
 *
 * Punto único de inicialización de la app. En tareas posteriores se inicializan aquí:
 * - RetrofitClient (T-103)
 * - Room / ShoppiDB (T-110)
 * - Firebase (FASE 3B) y WorkManager (sync)
 */
class ShoppiApp : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}
