package com.shoppi.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda la sesión del usuario (JWT propio + datos básicos) en SharedPreferences.
 *
 * El token se inyecta como `Authorization: Bearer {token}` por el interceptor de
 * Retrofit (T-103). Al hacer logout se limpia toda la sesión.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    /** Persiste la respuesta de `POST /api/auth/login|register`. */
    fun saveSession(token: String, userId: Long, nombre: String, email: String) {
        prefs.edit()
            .putString(Constants.KEY_TOKEN, token)
            .putLong(Constants.KEY_USER_ID, userId)
            .putString(Constants.KEY_USER_NAME, nombre)
            .putString(Constants.KEY_USER_EMAIL, email)
            .apply()
    }

    fun getToken(): String? = prefs.getString(Constants.KEY_TOKEN, null)

    fun getUserId(): Long = prefs.getLong(Constants.KEY_USER_ID, 0L)

    fun getUserName(): String? = prefs.getString(Constants.KEY_USER_NAME, null)

    fun getUserEmail(): String? = prefs.getString(Constants.KEY_USER_EMAIL, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    /** Cierra sesión (logout). */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
