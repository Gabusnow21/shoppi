package com.shoppi.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Movimiento de inventario (entrada o salida manual de stock).
 *
 * El id local es un UUID para poder registrar el movimiento sin conexión;
 * [remoteId] se completa cuando `POST /api/inventario/movimiento` lo confirma.
 * El backend trabaja con ids numéricos, por eso [productoId] guarda el id local
 * del producto y el nombre se desnormaliza para listar sin joins.
 */
@Entity(
    tableName = "inventario",
    indices = [
        Index(value = ["usuarioId"]),
        Index(value = ["remoteId"], unique = true)
    ]
)
data class MovimientoInventario(
    @PrimaryKey
    val id: String,
    val remoteId: Long? = null,
    val usuarioId: String,
    /** Id local del producto; puede ser null si el producto ya no existe. */
    val productoId: String? = null,
    val productoNombre: String = "",
    val tipoMovimiento: TipoMovimiento = TipoMovimiento.ENTRADA,
    val cantidad: Int = 0,
    val fecha: Long,
    val descripcion: String = "",
    val syncState: SyncState = SyncState.PENDING
)
