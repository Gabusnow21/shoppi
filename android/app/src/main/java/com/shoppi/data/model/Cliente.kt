package com.shoppi.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cliente del comercio.
 *
 * Mismo patrón offline-first que [Producto]: id local UUID, `remoteId` opcional
 * y [SyncState] para saber si el cambio ya llegó a PostgreSQL.
 */
@Entity(
    tableName = "clientes",
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class Cliente(
    @PrimaryKey
    val id: String,
    val remoteId: Long? = null,
    val usuarioId: String,
    val nombre: String,
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val deleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING
)
