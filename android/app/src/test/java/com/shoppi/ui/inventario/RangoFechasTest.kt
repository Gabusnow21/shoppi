package com.shoppi.ui.inventario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Pruebas del filtro por fecha del historial de inventario (función pura). */
class RangoFechasTest {

    private val zona = ZoneId.systemDefault()

    // 2026-10-26 15:30 hora local, un lunes.
    private val instante: Long = LocalDate.of(2026, 10, 26)
        .atTime(15, 30)
        .atZone(zona)
        .toInstant()
        .toEpochMilli()

    @Test
    fun `todo no impone ningun limite de fecha`() {
        val (inicio, fin) = rangoAFechas(RangoFiltro.TODO, instante)

        assertNull(inicio)
        assertNull(fin)
    }

    @Test
    fun `hoy cubre desde el inicio hasta el final del dia`() {
        val (inicio, fin) = rangoAFechas(RangoFiltro.HOY, instante)

        assertEquals(
            LocalDate.of(2026, 10, 26).atStartOfDay(zona).toInstant().toEpochMilli(),
            inicio
        )
        assertEquals(
            LocalDate.of(2026, 10, 26).atTime(LocalTime.MAX).atZone(zona).toInstant().toEpochMilli(),
            fin
        )
    }

    @Test
    fun `los ultimos 7 dias arrancan hace 6 dias`() {
        val (inicio, fin) = rangoAFechas(RangoFiltro.SIETE_DIAS, instante)

        assertEquals(
            LocalDate.of(2026, 10, 20).atStartOfDay(zona).toInstant().toEpochMilli(),
            inicio
        )
        assertEquals(
            LocalDate.of(2026, 10, 26).atTime(LocalTime.MAX).atZone(zona).toInstant().toEpochMilli(),
            fin
        )
    }

    @Test
    fun `los ultimos 30 dias arrancan hace 29 dias`() {
        val (inicio, _) = rangoAFechas(RangoFiltro.TREINTA_DIAS, instante)

        assertEquals(
            LocalDate.of(2026, 9, 27).atStartOfDay(zona).toInstant().toEpochMilli(),
            inicio
        )
    }
}
