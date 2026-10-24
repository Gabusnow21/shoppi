package com.shoppi.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas del badge de stock usado en la lista de productos. */
class StockBadgeTest {

    @Test
    fun `sin unidades el stock esta agotado`() {
        assertEquals(NivelStock.AGOTADO, nivelStock(stockActual = 0, stockMinimo = 5))
    }

    @Test
    fun `igual o por debajo del minimo es stock bajo`() {
        assertEquals(NivelStock.BAJO, nivelStock(stockActual = 5, stockMinimo = 5))
        assertEquals(NivelStock.BAJO, nivelStock(stockActual = 3, stockMinimo = 5))
    }

    @Test
    fun `por encima del minimo esta disponible`() {
        assertEquals(NivelStock.DISPONIBLE, nivelStock(stockActual = 6, stockMinimo = 5))
    }
}
