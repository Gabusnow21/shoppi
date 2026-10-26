package com.shoppi.data.repository

import com.shoppi.data.model.PeriodoReporte
import com.shoppi.data.remote.dto.ProductoInventarioResponse
import com.shoppi.data.remote.dto.ProductoMasVendidoResponse
import com.shoppi.data.remote.dto.ReporteInventarioResponse
import com.shoppi.data.remote.dto.ReporteVentasResponse
import com.shoppi.data.remote.dto.VentaPorDiaResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Pruebas de los mapeos DTO de reportes a los modelos de la app. */
class ReporteMappersTest {

    @Test
    fun `el reporte de ventas se convierte a modelo con periodo y fechas`() {
        val reporte = ReporteVentasResponse(
            periodo = "SEMANAL",
            fechaInicio = "2026-10-01",
            fechaFin = "2026-10-31",
            totalVentas = 42L,
            montoTotal = 1234.5,
            ventasPorDia = listOf(
                VentaPorDiaResponse(fecha = "2026-10-24", cantidad = 3L, monto = 90.0)
            )
        ).aModelo()

        assertEquals(PeriodoReporte.SEMANAL, reporte.periodo)
        assertEquals(LocalDate.of(2026, 10, 1), reporte.fechaInicio)
        assertEquals(LocalDate.of(2026, 10, 31), reporte.fechaFin)
        assertEquals(42L, reporte.totalVentas)
        assertEquals(1234.5, reporte.montoTotal, 0.001)
        assertEquals(1, reporte.ventasPorDia.size)
        assertEquals(LocalDate.of(2026, 10, 24), reporte.ventasPorDia.first().fecha)
        assertEquals(3L, reporte.ventasPorDia.first().cantidad)
    }

    @Test
    fun `un periodo desconocido cae en diario y una fecha invalida queda nula`() {
        val reporte = ReporteVentasResponse(
            periodo = "trimestral",
            fechaInicio = "no-es-fecha",
            fechaFin = null
        ).aModelo()

        assertEquals(PeriodoReporte.DIARIO, reporte.periodo)
        assertNull(reporte.fechaInicio)
        assertNull(reporte.fechaFin)
        assertEquals(0L, reporte.totalVentas)
        assertTrue(reporte.ventasPorDia.isEmpty())
    }

    @Test
    fun `el periodo viaja al backend en minusculas`() {
        assertEquals("mensual", PeriodoReporte.MENSUAL.parametro)
        assertEquals("diario", PeriodoReporte.DIARIO.parametro)
    }

    @Test
    fun `el top de productos conserva montos y cantidades`() {
        val top = ProductoMasVendidoResponse(
            productoId = 7L,
            nombre = "Arroz",
            cantidadVendida = 30L,
            montoTotal = 45.5
        ).aModelo()

        assertEquals(7L, top.productoId)
        assertEquals("Arroz", top.nombre)
        assertEquals(30L, top.cantidadVendida)
        assertEquals(45.5, top.montoTotal, 0.001)
    }

    @Test
    fun `el reporte de inventario marca el stock bajo`() {
        val reporte = ReporteInventarioResponse(
            totalProductos = 2,
            productosStockBajo = 1,
            valorInventario = 999.9,
            productos = listOf(
                ProductoInventarioResponse(
                    productoId = 1L,
                    nombre = "Arroz",
                    stockActual = 2,
                    stockMinimo = 5,
                    precio = 1.5,
                    valorStock = 3.0,
                    stockBajo = true
                ),
                ProductoInventarioResponse(
                    productoId = 2L,
                    nombre = "Aceite",
                    stockActual = 50,
                    stockMinimo = 5,
                    precio = 3.0,
                    valorStock = 150.0,
                    stockBajo = false
                )
            )
        ).aModelo()

        assertEquals(2, reporte.totalProductos)
        assertEquals(1, reporte.productosStockBajo)
        assertEquals(999.9, reporte.valorInventario, 0.001)
        assertTrue(reporte.productos.first().stockBajo)
        assertFalse(reporte.productos.last().stockBajo)
    }
}
