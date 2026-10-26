package com.shoppi.sync

import com.shoppi.data.model.Cliente
import com.shoppi.data.model.MetodoPago
import com.shoppi.data.model.MovimientoInventario
import com.shoppi.data.model.Producto
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.TipoMovimiento
import com.shoppi.data.model.Venta
import com.shoppi.data.model.VentaItem
import com.shoppi.data.remote.dto.ClienteRequest
import com.shoppi.data.remote.dto.ClienteResponse
import com.shoppi.data.remote.dto.MovimientoRequest
import com.shoppi.data.remote.dto.MovimientoResponse
import com.shoppi.data.remote.dto.ProductoRequest
import com.shoppi.data.remote.dto.ProductoResponse
import com.shoppi.data.remote.dto.VentaItemRequest
import com.shoppi.data.remote.dto.VentaRequest
import com.shoppi.data.remote.dto.VentaResponse
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Mapeos puros entre los modelos locales y los DTOs de la API REST.
 *
 * No dependen de Android ni de Retrofit: por eso se prueban directamente en JVM
 * (`SyncMappersTest`). El estado de sincronización nunca viaja en el DTO; es un
 * concepto exclusivamente local.
 */

/** Referencia local de un producto usada al reconstruir una venta remota. */
data class ProductoRemotoLocal(val idLocal: String, val nombre: String)

/** Referencia local de un cliente usada al reconstruir una venta remota. */
data class ClienteRemotoLocal(val idLocal: String, val nombre: String)

// ------------------------------------------------------------------- productos

fun Producto.toRequest(): ProductoRequest = ProductoRequest(
    nombre = nombre,
    descripcion = descripcion.ifBlank { null },
    precio = precio,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    categoria = categoria.ifBlank { null },
    imagenUrl = imagenUrl
)

fun ProductoResponse.toLocal(
    usuarioId: String,
    idLocal: String = UUID.randomUUID().toString(),
    ahora: Long = System.currentTimeMillis()
): Producto = Producto(
    id = idLocal,
    remoteId = id,
    usuarioId = usuarioId,
    nombre = nombre,
    descripcion = descripcion.orEmpty(),
    precio = precio,
    stockActual = stockActual,
    stockMinimo = stockMinimo,
    categoria = categoria.orEmpty(),
    imagenUrl = imagenUrl,
    deleted = false,
    createdAt = ahora,
    updatedAt = ahora,
    syncState = SyncState.SYNCED
)

// -------------------------------------------------------------------- clientes

fun Cliente.toRequest(): ClienteRequest = ClienteRequest(
    nombre = nombre,
    telefono = telefono.ifBlank { null },
    email = email.ifBlank { null },
    direccion = direccion.ifBlank { null }
)

fun ClienteResponse.toLocal(
    usuarioId: String,
    idLocal: String = UUID.randomUUID().toString(),
    ahora: Long = System.currentTimeMillis()
): Cliente = Cliente(
    id = idLocal,
    remoteId = id,
    usuarioId = usuarioId,
    nombre = nombre,
    telefono = telefono.orEmpty(),
    email = email.orEmpty(),
    direccion = direccion.orEmpty(),
    deleted = false,
    createdAt = ahora,
    updatedAt = ahora,
    syncState = SyncState.SYNCED
)

// ---------------------------------------------------------------------- ventas

/**
 * Convierte la venta local al cuerpo de `POST /api/ventas`.
 *
 * Los ids locales son UUID y el servidor espera ids numéricos, por eso recibe
 * las funciones de resolución. Si un producto aún no tiene `remoteId`, se lanza
 * [IllegalStateException]: el `SyncManager` empuja primero los productos, así que
 * solo ocurre ante datos corruptos y la venta queda en `FAILED`.
 */
fun Venta.toRequest(
    clienteRemoteId: Long?,
    productoRemoteId: (String?) -> Long?
): VentaRequest {
    val detalles = items.map { item ->
        val idRemoto = productoRemoteId(item.productoId)
            ?: throw IllegalStateException(
                "El producto '${item.productoNombre}' todavía no tiene id remoto."
            )
        VentaItemRequest(
            productoId = idRemoto,
            cantidad = item.cantidad,
            precioUnitario = item.precioUnitario
        )
    }

    if (detalles.isEmpty()) {
        throw IllegalStateException("La venta ${id} no tiene líneas.")
    }

    return VentaRequest(
        clienteId = clienteRemoteId,
        metodoPago = metodoPago.name,
        detalles = detalles,
        notas = notas.ifBlank { null }
    )
}

fun VentaResponse.toLocal(
    usuarioId: String,
    idLocal: String = UUID.randomUUID().toString(),
    clienteLocal: (Long) -> ClienteRemotoLocal? = { null },
    productoLocal: (Long) -> ProductoRemotoLocal? = { null },
    ahora: Long = System.currentTimeMillis()
): Venta {
    val cliente = clienteId?.let(clienteLocal)
    val itemsLocales = detalles.map { detalle ->
        val producto = productoLocal(detalle.productoId)
        VentaItem(
            productoId = producto?.idLocal,
            productoNombre = producto?.nombre.orEmpty(),
            cantidad = detalle.cantidad,
            precioUnitario = detalle.precioUnitario
        )
    }

    val fechaRemota = fecha.aEpochMillis()

    return Venta(
        id = idLocal,
        remoteId = id,
        usuarioId = usuarioId,
        clienteId = cliente?.idLocal,
        clienteNombre = cliente?.nombre.orEmpty(),
        items = itemsLocales,
        subtotal = subtotal,
        impuestos = impuestos,
        total = total,
        metodoPago = metodoPago.aMetodoPago(),
        notas = "",
        fecha = fechaRemota.takeIf { it > 0L } ?: ahora,
        updatedAt = ahora,
        syncState = SyncState.SYNCED
    )
}

// ---------------------------------------------------------------- inventario

/**
 * Convierte el movimiento local al cuerpo de `POST /api/inventario/movimiento`.
 *
 * El servidor trabaja con ids numéricos: si el producto todavía no tiene
 * `remoteId` se lanza [IllegalStateException] (el `SyncManager` empuja los
 * productos antes) y el movimiento queda en `FAILED`.
 */
fun MovimientoInventario.toRequest(
    productoRemoteId: (String?) -> Long?
): MovimientoRequest {
    val idRemoto = productoRemoteId(productoId)
        ?: throw IllegalStateException(
            "El producto '$productoNombre' todavía no tiene id remoto."
        )

    return MovimientoRequest(
        productoId = idRemoto,
        tipoMovimiento = tipoMovimiento.name,
        cantidad = cantidad,
        descripcion = descripcion.ifBlank { null }
    )
}

/**
 * Reconstruye un movimiento devuelto por la API.
 *
 * El historial es append-only y no tiene conflictos posibles: siempre gana el
 * registro remoto y se guarda como `SYNCED`.
 */
fun MovimientoResponse.toLocal(
    usuarioId: String,
    productoLocal: (Long) -> ProductoRemotoLocal? = { null },
    idLocal: String = UUID.randomUUID().toString(),
    ahora: Long = System.currentTimeMillis()
): MovimientoInventario {
    val producto = productoLocal(productoId)
    val nombreRemoto = productoNombre?.takeIf { it.isNotBlank() }

    return MovimientoInventario(
        id = idLocal,
        remoteId = id,
        usuarioId = usuarioId,
        productoId = producto?.idLocal,
        productoNombre = nombreRemoto ?: producto?.nombre.orEmpty(),
        tipoMovimiento = tipoMovimiento.aTipoMovimiento(),
        cantidad = cantidad,
        fecha = fecha.aEpochMillis().takeIf { it > 0L } ?: ahora,
        descripcion = descripcion.orEmpty(),
        syncState = SyncState.SYNCED
    )
}

// ---------------------------------------------------------------- conversiones

/** Interpreta la fecha ISO-8601 del backend; si no es válida usa 0. */
private fun String?.aEpochMillis(): Long {
    val texto = this ?: return 0L
    if (texto.isBlank()) return 0L

    return runCatching {
        OffsetDateTime.parse(texto).toInstant().toEpochMilli()
    }.getOrElse {
        runCatching {
            LocalDateTime.parse(texto)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrDefault(0L)
    }
}

private fun String?.aMetodoPago(): MetodoPago {
    val texto = this ?: return MetodoPago.EFECTIVO
    return runCatching { MetodoPago.valueOf(texto) }.getOrDefault(MetodoPago.EFECTIVO)
}

private fun String?.aTipoMovimiento(): TipoMovimiento {
    val texto = this ?: return TipoMovimiento.ENTRADA
    return runCatching { TipoMovimiento.valueOf(texto) }.getOrDefault(TipoMovimiento.ENTRADA)
}
