package com.shoppi

import android.app.Application

/**
 * Punto de entrada de la app.
 *
 * Construye el [AppContainer] y programa la sincronización periódica de
 * WorkManager (cada 15 minutos, solo con red).
 */
class ShoppiApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.programarSyncPeriodico()
    }
}
