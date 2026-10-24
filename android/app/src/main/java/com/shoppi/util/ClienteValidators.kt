package com.shoppi.util

/** Errores posibles del formulario de cliente. */
enum class ErrorCliente {
    NOMBRE_CORTO,
    EMAIL_INVALIDO,
    TELEFONO_INVALIDO
}

/**
 * Validaciones del cliente. El email y el teléfono son opcionales, pero si se
 * escriben deben tener un formato razonable.
 */
object ClienteValidators {

    private const val NOMBRE_MINIMO = 3
    private val TELEFONO_REGEX = Regex("^[0-9+()\\-\\s]{7,20}$")

    fun errores(nombre: String, telefono: String?, email: String?): Set<ErrorCliente> {
        val errores = mutableSetOf<ErrorCliente>()

        if (nombre.trim().length < NOMBRE_MINIMO) {
            errores.add(ErrorCliente.NOMBRE_CORTO)
        }
        if (!email.isNullOrBlank() && !Validators.emailValido(email)) {
            errores.add(ErrorCliente.EMAIL_INVALIDO)
        }
        if (!telefono.isNullOrBlank() && !TELEFONO_REGEX.matches(telefono.trim())) {
            errores.add(ErrorCliente.TELEFONO_INVALIDO)
        }
        return errores
    }
}
