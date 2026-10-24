package com.shoppi.sync

import com.shoppi.data.model.Cliente
import com.shoppi.data.model.Producto
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.Venta

/**
 * Política de resolución de conflictos: **last-write-wins** por `updatedAt`.
 *
 * Reglas:
 * 1. Si no existe copia local, se adopta la remota como [SyncState.SYNCED].
 * 2. Si la copia local no está `SYNCED` (tiene cambios `PENDING` o `FAILED`),
 *    siempre gana el local: un cambio del usuario nunca se descarta antes de
 *    haberse intentado subir.
 * 3. Si ambas están `SYNCED`, gana la de `updatedAt` más reciente.
 * 4. Nunca se mezclan registros de dueños distintos.
 * 5. Al adoptar la remota se conserva la PK local (UUID) y su `remoteId`, para
 *    no romper las referencias locales (por ejemplo, ventas → productos).
 *
 * Funciona en JVM puro: sin Android ni Retrofit, de ahí que sus pruebas sean
 * unitarias y rápidas.
 */
object SyncPolicy {

    fun resolverProducto(local: Producto?, remoto: Producto): Producto? {
        if (local == null) return remoto.copy(syncState = SyncState.SYNCED)
        if (local.usuarioId != remoto.usuarioId) return null
        if (local.syncState != SyncState.SYNCED) return null
        if (remoto.updatedAt <= local.updatedAt) return null

        return remoto.copy(
            id = local.id,
            remoteId = remoto.remoteId ?: local.remoteId,
            createdAt = local.createdAt,
            syncState = SyncState.SYNCED
        )
    }

    fun resolverCliente(local: Cliente?, remoto: Cliente): Cliente? {
        if (local == null) return remoto.copy(syncState = SyncState.SYNCED)
        if (local.usuarioId != remoto.usuarioId) return null
        if (local.syncState != SyncState.SYNCED) return null
        if (remoto.updatedAt <= local.updatedAt) return null

        return remoto.copy(
            id = local.id,
            remoteId = remoto.remoteId ?: local.remoteId,
            createdAt = local.createdAt,
            syncState = SyncState.SYNCED
        )
    }

    fun resolverVenta(local: Venta?, remoto: Venta): Venta? {
        if (local == null) return remoto.copy(syncState = SyncState.SYNCED)
        if (local.usuarioId != remoto.usuarioId) return null
        if (local.syncState != SyncState.SYNCED) return null
        if (remoto.updatedAt <= local.updatedAt) return null

        return remoto.copy(
            id = local.id,
            remoteId = remoto.remoteId ?: local.remoteId,
            syncState = SyncState.SYNCED
        )
    }
}
