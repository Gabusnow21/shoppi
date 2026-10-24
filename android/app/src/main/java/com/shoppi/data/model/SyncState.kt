package com.shoppi.data.model

/**
 * Estado de sincronización de un registro local.
 *
 * Es la pieza central del requisito offline-first: todo registro nace en Room
 * como [PENDING] y solo pasa a [SYNCED] cuando la API REST confirma el cambio.
 * Si falla por red se queda [PENDING] para reintentarse; si falla por una causa
 * distinta (validación, 4xx) pasa a [FAILED] y requiere corrección manual.
 */
enum class SyncState {
    PENDING,
    SYNCED,
    FAILED;

    companion object {
        /** Tolerante a valores desconocidos para no romper lecturas antiguas. */
        fun fromStorage(valor: String?): SyncState =
            entries.firstOrNull { it.name == valor } ?: PENDING
    }
}
