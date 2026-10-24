package com.shoppi.data.remote.dto

/** Línea del cuerpo de `POST /api/ventas` (ids del servidor). */
data class VentaItemRequest(
    val productoId: Long,
    val cantidad: Int,
    val precioUnitario: Double
)

/** Cuerpo de `POST /api/ventas`. */
data class VentaRequest(
    val clienteId: Long? = null,
    val metodoPago: String,
    val detalles: List<VentaItemRequest>,
    val notas: String? = null
)

/** Línea devuelta en el detalle de una venta. */
data class VentaItemResponse(
    val productoId: Long = 0,
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0
)

/** Venta devuelta por la API. `fecha` llega como ISO-8601. */
data class VentaResponse(
    val id: Long,
    val fecha: String? = null,
    val subtotal: Double = 0.0,
    val impuestos: Double = 0.0,
    val total: Double = 0.0,
    val metodoPago: String? = null,
    val clienteId: Long? = null,
    val detalles: List<VentaItemResponse> = emptyList()
)
