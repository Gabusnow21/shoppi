package com.shoppi.data.remote.dto

/** Cuerpo de alta/edición de cliente (`POST/PUT /api/clientes`). */
data class ClienteRequest(
    val nombre: String,
    val telefono: String? = null,
    val email: String? = null,
    val direccion: String? = null
)

/** Cliente devuelto por la API. */
data class ClienteResponse(
    val id: Long,
    val nombre: String = "",
    val telefono: String? = null,
    val email: String? = null,
    val direccion: String? = null
)
