package com.shoppi.data.remote.dto

/** Ventas acumuladas de un día (`ventasPorDia`). */
data class VentaPorDiaResponse(
    val fecha: String? = null,
    val cantidad: Long = 0,
    val monto: Double = 0.0
)

/** Reporte de ventas por período de `GET /api/reportes/ventas`. */
data class ReporteVentasResponse(
    val periodo: String? = null,
    val fechaInicio: String? = null,
    val fechaFin: String? = null,
    val totalVentas: Long = 0,
    val montoTotal: Double = 0.0,
    val ventasPorDia: List<VentaPorDiaResponse> = emptyList()
)

/** Producto de `GET /api/reportes/productos-mas-vendidos`. */
data class ProductoMasVendidoResponse(
    val productoId: Long = 0,
    val nombre: String = "",
    val cantidadVendida: Long = 0,
    val montoTotal: Double = 0.0
)

/** Producto dentro del reporte de estado de inventario. */
data class ProductoInventarioResponse(
    val productoId: Long = 0,
    val nombre: String = "",
    val stockActual: Int = 0,
    val stockMinimo: Int = 0,
    val precio: Double = 0.0,
    val valorStock: Double = 0.0,
    val stockBajo: Boolean = false
)

/** Reporte de `GET /api/reportes/inventario`. */
data class ReporteInventarioResponse(
    val totalProductos: Int = 0,
    val productosStockBajo: Int = 0,
    val valorInventario: Double = 0.0,
    val productos: List<ProductoInventarioResponse> = emptyList()
)
