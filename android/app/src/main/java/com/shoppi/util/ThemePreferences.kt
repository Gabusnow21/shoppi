package com.shoppi.util

import android.content.Context
import com.shoppi.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencia de tema persistida con `SharedPreferences`.
 *
 * Expone el modo actual como [StateFlow] para que `MainActivity` lo observe y
 * reconstruya el tema al instante; el valor sobrevive al reinicio de la app.
 */
class ThemePreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _modo = MutableStateFlow(leer())
    val modo: StateFlow<ThemeMode> = _modo.asStateFlow()

    fun seleccionar(modo: ThemeMode) {
        // `commit` (síncrono) en vez de `apply`: la preferencia es un cambio
        // puntual del usuario y debe quedar en disco antes de cualquier
        // force-stop/cierre abrupto para no perder el modo elegido.
        prefs.edit().putString(KEY_MODO, modo.name).commit()
        _modo.value = modo
    }

    private fun leer(): ThemeMode =
        ThemeMode.fromStorage(prefs.getString(KEY_MODO, null))

    private companion object {
        const val PREFS = "shoppi_ajustes"
        const val KEY_MODO = "tema"
    }
}
