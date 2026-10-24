package com.shoppi.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de las validaciones de producto. */
class ProductoValidatorsTest {

    private fun formulario(
        nombre: String = "Arroz",
        precio: String = "1.50",
        stockActual: String = "10",
        stockMinimo: String = "5"
    ) = ProductoValidators.FormularioProducto(nombre, precio, stockActual, stockMinimo)

    @Test
    fun `un producto bien formado no tiene errores`() {
        assertTrue(ProductoValidators.errores(formulario()).isEmpty())
    }

    @Test
    fun `el nombre demasiado corto se rechaza`() {
        assertTrue(
            ProductoValidators.errores(formulario(nombre = "A"))
                .contains(ErrorProducto.NOMBRE_CORTO)
        )
    }

    @Test
    fun `el precio invalido o negativo se rechaza`() {
        assertTrue(
            ProductoValidators.errores(formulario(precio = "abc"))
                .contains(ErrorProducto.PRECIO_INVALIDO)
        )
        assertTrue(
            ProductoValidators.errores(formulario(precio = "-1.50"))
                .contains(ErrorProducto.PRECIO_INVALIDO)
        )
    }

    @Test
    fun `el stock invalido o negativo se rechaza`() {
        assertTrue(
            ProductoValidators.errores(formulario(stockActual = "-1"))
                .contains(ErrorProducto.STOCK_INVALIDO)
        )
        assertTrue(
            ProductoValidators.errores(formulario(stockMinimo = "x"))
                .contains(ErrorProducto.STOCK_MINIMO_INVALIDO)
        )
    }

    @Test
    fun `acepta la coma decimal y construye el producto`() {
        val producto = ProductoValidators.aProducto(
            formulario = formulario(precio = "1,50", stockActual = "3", stockMinimo = "1"),
            id = "prod-1",
            usuarioId = "uid-1",
            ahora = 100L
        )

        assertEquals(1.50, producto?.precio ?: 0.0, 0.001)
        assertEquals("uid-1", producto?.usuarioId)
        assertEquals(100L, producto?.updatedAt)
    }

    @Test
    fun `un formulario invalido no construye el producto`() {
        assertNull(
            ProductoValidators.aProducto(
                formulario = formulario(nombre = ""),
                id = "prod-1",
                usuarioId = "uid-1",
                ahora = 100L
            )
        )
    }

    @Test
    fun `la revalidacion tipada detecta valores negativos`() {
        assertTrue(
            ProductoValidators.errores("Arroz", -1.0, 0, 0)
                .contains(ErrorProducto.PRECIO_INVALIDO)
        )
        assertTrue(
            ProductoValidators.errores("Arroz", 1.0, -1, 0)
                .contains(ErrorProducto.STOCK_INVALIDO)
        )
        assertTrue(
            ProductoValidators.errores("Arroz", 1.0, 0, -1)
                .contains(ErrorProducto.STOCK_MINIMO_INVALIDO)
        )
    }
}
