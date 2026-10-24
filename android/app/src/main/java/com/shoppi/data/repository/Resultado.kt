package com.shoppi.data.repository

/**
 * Resultado genérico de las operaciones de repositorio.
 *
 * Evita propagar excepciones a la UI: el ViewModel recibe [Ok] o [Error] y
 * decide qué mostrar.
 */
sealed class Resultado<out T> {
    data class Ok<T>(val datos: T) : Resultado<T>()
    data class Error(val mensaje: String, val causa: Throwable? = null) : Resultado<Nothing>()

    val esOk: Boolean get() = this is Ok
    val esError: Boolean get() = this is Error

    fun <R> map(transformacion: (T) -> R): Resultado<R> = when (this) {
        is Ok -> Ok(transformacion(datos))
        is Error -> this
    }
}
