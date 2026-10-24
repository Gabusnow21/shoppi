package com.shoppi.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Producto del catálogo.
 *
 * El id local es un UUID para poder crear productos sin conexión; [remoteId] se
 * completa cuando la API confirma el alta. El binario de la imagen no se guarda
 * aquí: solo la URL pública (`imagenUrl`), según el contrato de la API.
 */
@Entity(
    tableName = "productos",
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class Producto(
    @PrimaryKey
    val id: String,
    val remoteId: Long? = null,
    val usuarioId: String,
    val nombre: String,
    val descripcion: String = "",
    val precio: Double,
    val stockActual: Int = 0,
    val stockMinimo: Int = 5,
    val categoria: String = "",
    val imagenUrl: String? = null,
    /** Borrado lógico: se propaga como un DELETE y recién después se purga. */
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING
)
