package com.shoppi.util

/**
 * Validaciones de autenticación: correo, contraseña y nombre completo.
 *
 * Implementadas en Kotlin puro (sin `android.util.Patterns`) para poder probarlas
 * con JUnit en la JVM.
 */
object Validators {

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    /** Contraseña alfanumérica de mínimo 8 caracteres (sin símbolos ni espacios). */
    private val PASSWORD_REGEX = Regex("^[A-Za-z0-9]{8,}$")

    const val PASSWORD_MIN_LENGTH = 8
    const val NOMBRE_MIN_LENGTH = 7

    fun emailValido(email: String?): Boolean =
        !email.isNullOrBlank() && EMAIL_REGEX.matches(email.trim())

    fun contrasenaValida(contrasena: String?): Boolean =
        !contrasena.isNullOrBlank() && PASSWORD_REGEX.matches(contrasena)

    fun nombreCompletoValido(nombre: String?): Boolean =
        !nombre.isNullOrBlank() && nombre.trim().length >= NOMBRE_MIN_LENGTH

    fun contrasenasCoinciden(primera: String?, segunda: String?): Boolean =
        !primera.isNullOrEmpty() && primera == segunda

    /** Nombre de cliente del punto de venta (mínimo 3 caracteres). */
    fun clienteValido(cliente: String?): Boolean =
        !cliente.isNullOrBlank() && cliente.trim().length >= 3
}
