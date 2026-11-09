package com.shoppi.util

import android.content.Context
import com.shoppi.data.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesión del usuario en SharedPreferences.
 *
 * Guarda el JWT, el id y datos mínimos del perfil. Con el chip "Recordar sesión"
 * activo se conservan además el hash y el salt de la contraseña para permitir el
 * login offline contra Room. La contraseña en claro **nunca** se guarda.
 */
class SessionManager(contexto: Context) {

    private val prefs =
        contexto.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Id del usuario activo como flujo.
     *
     * Los repositorios observan este flujo para que sus consultas a Room se
     * reinicien cuando se inicia o cierra sesión. Sin esto, un ViewModel creado
     * antes del login (por ejemplo el carrito compartido del punto de venta)
     * quedaba suscripto para siempre a un flujo vacío.
     */
    private val _idUsuario = MutableStateFlow(prefs.getString(KEY_ID, null))
    val idUsuarioFlow: StateFlow<String?> = _idUsuario.asStateFlow()

    var recordarSesion: Boolean
        get() = prefs.getBoolean(KEY_RECORDAR, false)
        private set(value) = prefs.edit().putBoolean(KEY_RECORDAR, value).apply()

    /**
     * Hay una sesión que debe restaurarse al abrir la app: solo si el chip estaba
     * activo. Mientras la app está abierta, [idUsuario] y [token] bastan.
     */
    fun haySesion(): Boolean = recordarSesion && !idUsuario().isNullOrBlank()

    fun token(): String? = prefs.getString(KEY_TOKEN, null)

    fun idUsuario(): String? = _idUsuario.value

    fun email(): String? = prefs.getString(KEY_EMAIL, null)

    fun nombreCompleto(): String? = prefs.getString(KEY_NOMBRE, null)

    fun passwordHash(): String? = prefs.getString(KEY_HASH, null)

    fun salt(): String? = prefs.getString(KEY_SALT, null)

    /**
     * Guarda la sesión activa.
     *
     * El id, nombre y JWT se guardan siempre (la sincronización los necesita
     * mientras la app vive); el hash y el salt solo si `recordar` es verdadero.
     * Un login offline deja el token vacío: se puede operar en local, pero para
     * sincronizar hace falta volver a iniciar sesión con red.
     */
    fun guardarSesion(usuario: Usuario, token: String, recordar: Boolean) {
        recordarSesion = recordar
        prefs.edit().apply {
            putString(KEY_ID, usuario.id)
            putString(KEY_EMAIL, usuario.email)
            putString(KEY_NOMBRE, usuario.nombreCompleto)
            putString(KEY_TOKEN, token)
            if (recordar) {
                putString(KEY_HASH, usuario.passwordHash)
                putString(KEY_SALT, usuario.salt)
            } else {
                remove(KEY_HASH)
                remove(KEY_SALT)
            }
            apply()
        }
        _idUsuario.value = usuario.id
    }

    /** Descarta la sesión activa sin borrar el email recordado. */
    fun limpiarSesionActiva() {
        recordarSesion = false
        prefs.edit().apply {
            remove(KEY_ID)
            remove(KEY_NOMBRE)
            remove(KEY_TOKEN)
            remove(KEY_HASH)
            remove(KEY_SALT)
            apply()
        }
        _idUsuario.value = null
    }

    fun actualizarNombre(nombre: String) {
        prefs.edit().putString(KEY_NOMBRE, nombre).apply()
    }

    fun actualizarCredenciales(passwordHash: String, salt: String) {
        if (!recordarSesion) return
        prefs.edit().putString(KEY_HASH, passwordHash).putString(KEY_SALT, salt).apply()
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
        _idUsuario.value = null
    }

    companion object {
        private const val PREFS = "shoppi_sesion"
        private const val KEY_ID = "usuario_id"
        private const val KEY_EMAIL = "usuario_email"
        private const val KEY_NOMBRE = "usuario_nombre"
        private const val KEY_TOKEN = "usuario_token"
        private const val KEY_HASH = "usuario_password_hash"
        private const val KEY_SALT = "usuario_salt"
        private const val KEY_RECORDAR = "recordar_sesion"
    }
}
