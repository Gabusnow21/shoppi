package com.shoppi.util

/**
 * Constantes globales de la app.
 *
 * `BASE_URL` respeta el contrato `api_specification.md` (§Base URL: `/api`).
 * En el emulador Android `10.0.2.2` apunta al host; en dispositivo físico usar
 * la IP LAN o la URL de IaaS. En FASE 3B se sobreescribe con Remote Config
 * (`api_base_url`) sin recompilar.
 */
object Constants {

    /** Default de desarrollo (emulador). Termina en `/` para Retrofit. */
    const val BASE_URL = "http://10.0.2.2:8080/api/"

    // --- SharedPreferences (sesión JWT) ---
    const val PREFS_NAME = "shoppi_prefs"
    const val KEY_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"

    // --- Auth header (api_specification.md §Autenticación) ---
    const val AUTH_HEADER = "Authorization"
    const val BEARER_PREFIX = "Bearer "

    // --- Timeouts HTTP (segundos) ---
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 30L
}
