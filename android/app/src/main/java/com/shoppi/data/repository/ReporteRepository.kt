package com.shoppi.data.repository

import com.shoppi.data.model.PeriodoReporte
import com.shoppi.data.model.ProductoEnStock
import com.shoppi.data.model.ReporteInventario
import com.shoppi.data.model.ReporteVentas
import com.shoppi.data.model.TopProducto
import com.shoppi.data.model.VentaPorDia
import com.shoppi.data.remote.RemoteDataSource
import com.shoppi.data.remote.dto.ProductoInventarioResponse
import com.shoppi.data.remote.dto.ProductoMasVendidoResponse
import com.shoppi.data.remote.dto.ReporteInventarioResponse
import com.shoppi.data.remote.dto.ReporteVentasResponse
import com.shoppi.data.remote.dto.VentaPorDiaResponse
import com.shoppi.sync.esErrorDeRed
import retrofit2.HttpException
import java.time.LocalDate

/**
 * Repositorio de reportes: **solo lectura** contra la API.
 *
 * Los agregados los calcula PostgreSQL, así que no hay tabla local ni
 * sincronización: cada consulta va a la red y devuelve `Resultado.Error` con un
 * mensaje claro cuando no hay conexión. Las fechas se mandan como `yyyy-MM-dd`
 * y llegan interpretadas como [LocalDate].
 */
class ReporteRepository(
    private val remote: RemoteDataSource,
) {

    /** Ventas agrupadas por día dentro del período. Sin fechas usa el default del backend. */
    suspend fun ventas(
        periodo: PeriodoReporte = PeriodoReporte.DIARIO,
        fechaInicio: LocalDate? = null,
        fechaFin: LocalDate? = null
    ): Resultado<ReporteVentas> = capturar {
        remote.reporteVentas(
            periodo = periodo.parametro,
            fechaInicio = fechaInicio.aTexto(),
            fechaFin = fechaFin.aTexto()
        ).aModelo()
    }

    /** Ranking de productos más vendidos. */
    suspend fun productosMasVendidos(
        limite: Int = 10,
        fechaInicio: LocalDate? = null,
        fechaFin: LocalDate? = null
    ): Resultado<List<TopProducto>> = capturar {
        remote.productosMasVendidos(
            limite = limite,
            fechaInicio = fechaInicio.aTexto(),
            fechaFin = fechaFin.aTexto()
        ).map { it.aModelo() }
    }

    /** Estado actual del inventario (stock, valor y faltantes). */
    suspend fun inventario(): Resultado<ReporteInventario> = capturar {
        remote.reporteInventario().aModelo()
    }

    private suspend fun <T> capturar(accion: suspend () -> T): Resultado<T> =
        try {
            Resultado.Ok(accion())
        } catch (error: Exception) {
            Resultado.Error(mensajeDeError(error), error)
        }

    private fun mensajeDeError(error: Throwable): String = when {
        esErrorDeRed(error) -> "Sin conexión: los reportes necesitan internet."
        error is HttpException && error.code() == 401 ->
            "Tu sesión expiró. Iniciá sesión de nuevo."
        else -> "No se pudieron cargar los reportes."
    }
}

// ------------------------------------------------------------------ mapeos

/** Cuerpo de la fecha ISO `yyyy-MM-dd`; `null` o inválida → sin fecha. */
private fun LocalDate?.aTexto(): String? = this?.toString()

fun ReporteVentasResponse.aModelo(): ReporteVentas = ReporteVentas(
    periodo = periodo.aPeriodo(),
    fechaInicio = fechaInicio.aFecha(),
    fechaFin = fechaFin.aFecha(),
    totalVentas = totalVentas,
    montoTotal = montoTotal,
    ventasPorDia = ventasPorDia.map { it.aModelo() }
)

fun VentaPorDiaResponse.aModelo(): VentaPorDia = VentaPorDia(
    fecha = fecha.aFecha(),
    cantidad = cantidad,
    monto = monto
)

fun ProductoMasVendidoResponse.aModelo(): TopProducto = TopProducto(
    productoId = productoId,
    nombre = nombre,
    cantidadVendida = cantidadVendida,
    montoTotal = montoTotal
)

fun ReporteInventarioResponse.aModelo(): ReporteInventario = ReporteInventario(
    totalProductos = totalProductos,
    productosStockBajo = productosStockBajo,
    valorInventario = valorInventario,
    productos = productos.map { it.aModelo() }
)

fun ProductoInventarioResponse.aModelo(): ProductoEnStock = ProductoEnStock(
    productoId = productoId,
    nombre = nombre,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    precio = precio,
    valorStock = valorStock,
    stockBajo = stockBajo
)

private fun String?.aPeriodo(): PeriodoReporte {
    val texto = this ?: return PeriodoReporte.DIARIO
    return PeriodoReporte.entries.firstOrNull {
        it.name.equals(texto, ignoreCase = true)
    } ?: PeriodoReporte.DIARIO
}

private fun String?.aFecha(): LocalDate? {
    val texto = this ?: return null
    if (texto.isBlank()) return null
    return runCatching { LocalDate.parse(texto) }.getOrNull()
}
