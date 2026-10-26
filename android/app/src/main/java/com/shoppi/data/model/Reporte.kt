package com.shoppi.data.model

import java.time.LocalDate

/**
 * Modelos de los reportes.
 *
 * Son agregados de solo lectura que el backend calcula sobre PostgreSQL: no
 * tienen entidad en Room ni `syncState`, porque nunca se escriben en el
 * dispositivo ni se sincronizan. Las fechas llegan como `LocalDate` ya
 * interpretadas desde el ISO-8601 de la API.
 */

/** Agrupación del reporte de ventas (`diario`, `semanal` o `mensual`). */
enum class PeriodoReporte(val etiqueta: String) {
    DIARIO("Diario"),
    SEMANAL("Semanal"),
    MENSUAL("Mensual");

    /** Valor en minúsculas que espera el query param `periodo`. */
    val parametro: String
        get() = name.lowercase()
}

/** Ventas acumuladas de un día dentro del rango consultado. */
data class VentaPorDia(
    val fecha: LocalDate?,
    val cantidad: Long,
    val monto: Double
)

/** Reporte de ventas por período. */
data class ReporteVentas(
    val periodo: PeriodoReporte,
    val fechaInicio: LocalDate?,
    val fechaFin: LocalDate?,
    val totalVentas: Long,
    val montoTotal: Double,
    val ventasPorDia: List<VentaPorDia>
)

/** Producto del ranking de más vendidos. */
data class TopProducto(
    val productoId: Long,
    val nombre: String,
    val cantidadVendida: Long,
    val montoTotal: Double
)

/** Estado de un producto dentro del reporte de inventario. */
data class ProductoEnStock(
    val productoId: Long,
    val nombre: String,
    val stockActual: Int,
    val stockMinimo: Int,
    val precio: Double,
    val valorStock: Double,
    val stockBajo: Boolean
)

/** Reporte de estado del inventario. */
data class ReporteInventario(
    val totalProductos: Int,
    val productosStockBajo: Int,
    val valorInventario: Double,
    val productos: List<ProductoEnStock>
)
