package com.shoppi.data.remote.dto

/**
 * Cuerpo de `POST /api/inventario/movimiento`.
 *
 * El backend espera el id numérico del producto (`productoId: Long`), no el
 * UUID local: la resolución la hace el `SyncManager`.
 */
data class MovimientoRequest(
    val productoId: Long,
    val tipoMovimiento: String,
    val cantidad: Int,
    val descripcion: String? = null
)

/** Movimiento devuelto por la API. `fecha` llega como ISO-8601. */
data class MovimientoResponse(
    val id: Long = 0,
    val productoId: Long = 0,
    val productoNombre: String? = null,
    val tipoMovimiento: String? = null,
    val cantidad: Int = 0,
    val fecha: String? = null,
    val descripcion: String? = null
)
