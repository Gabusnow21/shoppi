package com.shoppi.util

import com.shoppi.data.model.VentaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del cálculo de totales y validaciones del punto de venta.
 * El IVA de El Salvador es 13 %.
 */
class VentaCalculatorTest {

    @Test
    fun `dos items producen subtotal impuesto y total correctos`() {
        val items = listOf(
            VentaItem("prod-1", "Camiseta deportiva", 2, 12.50),
            VentaItem("prod-2", "Gorra", 1, 7.30)
        )

        val totales = VentaCalculator.calcularTotales(items)

        assertEquals(32.30, totales.subtotal, 0.001)
        assertEquals(4.20, totales.impuesto, 0.001)
        assertEquals(36.50, totales.total, 0.001)
    }

    @Test
    fun `el impuesto es exactamente el 13 por ciento`() {
        val totales = VentaCalculator.calcularTotales(
            listOf(VentaItem("prod-1", "Producto", 1, 100.0))
        )
        assertEquals(100.0, totales.subtotal, 0.001)
        assertEquals(13.0, totales.impuesto, 0.001)
        assertEquals(113.0, totales.total, 0.001)
    }

    @Test
    fun `sin items los tres totales son cero`() {
        val totales = VentaCalculator.calcularTotales(emptyList())
        assertEquals(0.0, totales.subtotal, 0.0)
        assertEquals(0.0, totales.impuesto, 0.0)
        assertEquals(0.0, totales.total, 0.0)
    }

    @Test
    fun `los importes de la linea son cantidad por precio`() {
        assertEquals(25.0, VentaItem("prod-1", "Camiseta", 2, 12.50).importe, 0.001)
        assertEquals(0.0, VentaItem("prod-1", "Regalo", 1, 0.0).importe, 0.001)
    }

    @Test
    fun `los valores se redondean a dos decimales`() {
        val totales = VentaCalculator.calcularTotales(
            listOf(VentaItem("prod-1", "Fruta", 3, 1.111111))
        )
        assertEquals(3.33, totales.subtotal, 0.001)
        assertEquals(0.43, totales.impuesto, 0.001)
        assertEquals(3.76, totales.total, 0.001)
    }

    @Test
    fun `el redondeo comercial deja dos decimales`() {
        assertEquals(4.20, VentaCalculator.redondear(4.199), 0.0)
        assertEquals(32.30, VentaCalculator.redondear(32.300000000000004), 0.0)
        assertEquals(0.0, VentaCalculator.redondear(0.0), 0.0)
    }

    // --------------------------------------------------------- validaciones

    @Test
    fun `una venta bien formada no tiene errores`() {
        val errores = VentaValidators.errores(
            listOf(VentaItem("prod-1", "Camiseta", 2, 12.50))
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `guardar sin items se rechaza`() {
        assertEquals(setOf(ErrorVenta.SIN_ITEMS), VentaValidators.errores(emptyList()))
    }

    @Test
    fun `items invalidos se rechazan`() {
        assertTrue(
            VentaValidators.errores(listOf(VentaItem(null, "Camiseta", 1, 10.0)))
                .contains(ErrorVenta.ITEMS_INVALIDOS)
        )
        assertTrue(
            VentaValidators.errores(listOf(VentaItem("prod-1", "Camiseta", 0, 10.0)))
                .contains(ErrorVenta.ITEMS_INVALIDOS)
        )
        assertTrue(
            VentaValidators.errores(listOf(VentaItem("prod-1", "Camiseta", 1, -1.0)))
                .contains(ErrorVenta.ITEMS_INVALIDOS)
        )
    }

    @Test
    fun `las filas validas se convierten a items tipados`() {
        val items = VentaValidators.aItems(
            listOf(
                VentaValidators.ItemFormulario("prod-1", "Camiseta", "2", "12.50"),
                VentaValidators.ItemFormulario("prod-2", "Gorra", "1", "7,30")
            )
        )
        assertEquals(2, items.size)
        assertEquals(2, items[0].cantidad)
        assertEquals(12.50, items[0].precioUnitario, 0.001)
        assertEquals(7.30, items[1].precioUnitario, 0.001)
    }

    @Test
    fun `las filas ilegibles se omiten para calcular los totales en vivo`() {
        val items = VentaValidators.aItems(
            listOf(
                VentaValidators.ItemFormulario("prod-1", "Camiseta", "2", "12.50"),
                VentaValidators.ItemFormulario("prod-2", "Gorra", "", "7.30")
            )
        )
        assertEquals(1, items.size)
        assertEquals("Camiseta", items[0].productoNombre)
    }

    @Test
    fun `el formulario y los items tipados rechazan los mismos datos`() {
        assertTrue(
            VentaValidators.erroresFormulario(
                listOf(VentaValidators.ItemFormulario("prod-1", "Camiseta", "2", "12.50"))
            ).isEmpty()
        )
        assertTrue(
            VentaValidators.errores(
                VentaValidators.aItems(
                    listOf(VentaValidators.ItemFormulario("prod-1", "Camiseta", "2", "12.50"))
                )
            ).isEmpty()
        )

        val filaMala = listOf(
            VentaValidators.ItemFormulario(null, "", "2", "12.50")
        )
        assertTrue(
            VentaValidators.erroresFormulario(filaMala).contains(ErrorVenta.ITEMS_INVALIDOS)
        )
    }
}
