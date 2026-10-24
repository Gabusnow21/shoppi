package com.shoppi.util

import com.shoppi.data.model.VentaItem

/** Errores posibles del carrito de venta. */
enum class ErrorVenta {
    SIN_ITEMS,
    ITEMS_INVALIDOS
}

/**
 * Validaciones de la venta (punto de venta).
 *
 * Se aplica tanto al texto crudo del carrito ([ItemFormulario]) como a los
 * ítems ya tipados ([VentaItem]) antes de escribir en Room.
 */
object VentaValidators {

    data class ItemFormulario(
        val productoId: String?,
        val productoNombre: String,
        val cantidad: String,
        val precioUnitario: String
    )

    fun errores(items: List<VentaItem>): Set<ErrorVenta> {
        if (items.isEmpty()) return setOf(ErrorVenta.SIN_ITEMS)
        if (items.any { !esItemValido(it) }) return setOf(ErrorVenta.ITEMS_INVALIDOS)
        return emptySet()
    }

    fun erroresFormulario(items: List<ItemFormulario>): Set<ErrorVenta> {
        if (items.isEmpty()) return setOf(ErrorVenta.SIN_ITEMS)
        if (items.any { !esFilaValida(it) }) return setOf(ErrorVenta.ITEMS_INVALIDOS)
        return emptySet()
    }

    /**
     * Convierte las filas válidas y omite las que aún no son interpretables.
     * Ese comportamiento tolerante alimenta los totales mientras se escribe.
     */
    fun aItems(items: List<ItemFormulario>): List<VentaItem> = items.mapNotNull { fila ->
        val cantidad = fila.cantidad.trim().toIntOrNull() ?: return@mapNotNull null
        val precio = fila.precioUnitario.aNumero() ?: return@mapNotNull null
        if (fila.productoId.isNullOrBlank() || fila.productoNombre.isBlank()) return@mapNotNull null
        if (cantidad < 1 || precio < 0.0) return@mapNotNull null

        VentaItem(
            productoId = fila.productoId,
            productoNombre = fila.productoNombre.trim(),
            cantidad = cantidad,
            precioUnitario = precio
        )
    }

    private fun esItemValido(item: VentaItem): Boolean =
        !item.productoId.isNullOrBlank() &&
            item.productoNombre.isNotBlank() &&
            item.cantidad >= 1 &&
            item.precioUnitario >= 0.0

    private fun esFilaValida(fila: ItemFormulario): Boolean =
        !fila.productoId.isNullOrBlank() &&
            fila.productoNombre.isNotBlank() &&
            fila.cantidad.trim().toIntOrNull()?.let { it >= 1 } == true &&
            fila.precioUnitario.aNumero()?.let { it >= 0.0 } == true

    private fun String?.aNumero(): Double? =
        this?.trim()?.replace(',', '.')?.toDoubleOrNull()
}
