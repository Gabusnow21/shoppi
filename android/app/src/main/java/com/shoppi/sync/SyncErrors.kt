package com.shoppi.sync

import retrofit2.HttpException
import java.io.IOException

/**
 * Detecta fallos de red o del servidor que conviene reintentar.
 *
 * Se comparte entre [SyncManager] y los repositorios para decidir el trato de un
 * error: si es de red (o el servidor está temporalmente caído), el registro
 * sigue `PENDING` y se reintenta más tarde; cualquier otro error (400, 401, 403,
 * 404, 422) se marca `FAILED` y requiere corrección del usuario.
 */
internal fun esErrorDeRed(error: Throwable): Boolean {
    var actual: Throwable? = error

    while (actual != null) {
        when (actual) {
            is IOException -> return true
            is HttpException -> {
                if (actual.code() in CODIGOS_REINTENTABLES) return true
            }
        }
        actual = actual.cause
    }

    return false
}

/** 408 timeout, 425 too early, 429 rate limit y errores 5xx de infraestructura. */
private val CODIGOS_REINTENTABLES = setOf(408, 425, 429, 500, 502, 503, 504)
