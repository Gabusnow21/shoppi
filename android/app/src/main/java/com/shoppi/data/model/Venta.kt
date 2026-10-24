package com.shoppi.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Venta del punto de venta.
 *
 * Los totales se calculan con [com.shoppi.util.VentaCalculator] y se guardan
 * desnormalizados para listar y reportar sin recalcular el JSON. La venta nace
 * en Room como [SyncState.PENDING] y el `SyncManager` la empuja a la API.
 */
@Entity(
    tableName = "ventas",
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class Venta(
    @PrimaryKey
    val id: String,
    val remoteId: Long? = null,
    val usuarioId: String,
    /** Id local del cliente (opcional). */
    val clienteId: String? = null,
    val clienteNombre: String = "",
    @ColumnInfo(name = "itemsJson")
    val items: List<VentaItem> = emptyList(),
    val subtotal: Double = 0.0,
    val impuestos: Double = 0.0,
    val total: Double = 0.0,
    val metodoPago: MetodoPago = MetodoPago.EFECTIVO,
    val notas: String = "",
    val fecha: Long,
    val updatedAt: Long,
    val syncState: SyncState = SyncState.PENDING
)
