package com.shoppi.data.remote.dto

/** Cuerpo de alta/edición de producto (`POST/PUT /api/productos`). */
data class ProductoRequest(
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val stockActual: Int,
    val stockMinimo: Int,
    val categoria: String? = null,
    val imagenUrl: String? = null
)

/** Producto devuelto por la API. */
data class ProductoResponse(
    val id: Long,
    val nombre: String = "",
    val descripcion: String? = null,
    val precio: Double = 0.0,
    val stockActual: Int = 0,
    val stockMinimo: Int = 0,
    val categoria: String? = null,
    val imagenUrl: String? = null
)
