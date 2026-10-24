package com.shoppi.util

import com.shoppi.data.model.Producto

/** Errores posibles del formulario de producto. */
enum class ErrorProducto {
    NOMBRE_CORTO,
    PRECIO_INVALIDO,
    STOCK_INVALIDO,
    STOCK_MINIMO_INVALIDO
}

/**
 * Validaciones del producto, en dos capas:
 * - [FormularioProducto] valida el texto crudo que escribe el usuario;
 * - [errores] sobre valores tipados revalida antes de escribir en Room.
 *
 * Kotlin puro para poder probarlo con JUnit.
 */
object ProductoValidators {

    private const val NOMBRE_MINIMO = 2

    data class FormularioProducto(
        val nombre: String,
        val precio: String,
        val stockActual: String,
        val stockMinimo: String
    )

    fun errores(formulario: FormularioProducto): Set<ErrorProducto> {
        val errores = mutableSetOf<ErrorProducto>()

        if (formulario.nombre.trim().length < NOMBRE_MINIMO) {
            errores.add(ErrorProducto.NOMBRE_CORTO)
        }
        if (formulario.precio.aNumero()?.let { it >= 0.0 } != true) {
            errores.add(ErrorProducto.PRECIO_INVALIDO)
        }
        if (formulario.stockActual.trim().toIntOrNull()?.let { it >= 0 } != true) {
            errores.add(ErrorProducto.STOCK_INVALIDO)
        }
        if (formulario.stockMinimo.trim().toIntOrNull()?.let { it >= 0 } != true) {
            errores.add(ErrorProducto.STOCK_MINIMO_INVALIDO)
        }
        return errores
    }

    fun errores(
        nombre: String,
        precio: Double,
        stockActual: Int,
        stockMinimo: Int
    ): Set<ErrorProducto> {
        val errores = mutableSetOf<ErrorProducto>()
        if (nombre.trim().length < NOMBRE_MINIMO) errores.add(ErrorProducto.NOMBRE_CORTO)
        if (precio < 0.0) errores.add(ErrorProducto.PRECIO_INVALIDO)
        if (stockActual < 0) errores.add(ErrorProducto.STOCK_INVALIDO)
        if (stockMinimo < 0) errores.add(ErrorProducto.STOCK_MINIMO_INVALIDO)
        return errores
    }

    /** Construye el producto local si el formulario es válido. */
    fun aProducto(
        formulario: FormularioProducto,
        id: String,
        usuarioId: String,
        ahora: Long
    ): Producto? {
        if (errores(formulario).isNotEmpty()) return null

        return Producto(
            id = id,
            usuarioId = usuarioId,
            nombre = formulario.nombre.trim(),
            precio = formulario.precio.aNumero() ?: return null,
            stockActual = formulario.stockActual.trim().toIntOrNull() ?: return null,
            stockMinimo = formulario.stockMinimo.trim().toIntOrNull() ?: return null,
            createdAt = ahora,
            updatedAt = ahora
        )
    }

    /** Acepta punto o coma decimal según el teclado del dispositivo. */
    private fun String?.aNumero(): Double? =
        this?.trim()?.replace(',', '.')?.toDoubleOrNull()
}
