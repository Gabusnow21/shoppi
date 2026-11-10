package com.shoppi

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Helper de observabilidad (Crashlytics + Analytics) auxiliar.
 */
object Observabilidad {

    fun setUsuario(context: Context, usuarioId: String) {
        val uid = "user-$usuarioId"
        FirebaseCrashlytics.getInstance().setUserId(uid)
        FirebaseAnalytics.getInstance(context).setUserId(uid)
    }

    fun logEvento(context: Context, nombre: String, params: Bundle? = null) {
        FirebaseAnalytics.getInstance(context).logEvent(nombre, params)
    }

    fun registrarError(error: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(error)
    }
}