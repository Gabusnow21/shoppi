package com.shoppi.sync

import com.shoppi.data.model.Cliente
import com.shoppi.data.model.Producto
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.Venta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas de la política de conflictos (last-write-wins).
 *
 * Cubren las reglas críticas: un cambio local pendiente nunca se pierde y el
 * remoto más reciente solo sobrescribe cuando lo local ya estaba sincronizado.
 */
class SyncPolicyTest {

    private fun producto(
        id: String = "prod-local-1",
        remoteId: Long? = 10L,
        usuarioId: String = "uid-1",
        updatedAt: Long = 100L,
        syncState: SyncState = SyncState.SYNCED,
        nombre: String = "Arroz"
    ) = Producto(
        id = id,
        remoteId = remoteId,
        usuarioId = usuarioId,
        nombre = nombre,
        precio = 1.5,
        createdAt = 1L,
        updatedAt = updatedAt,
        syncState = syncState
    )

    private fun cliente(
        id: String = "cli-local-1",
        remoteId: Long? = 20L,
        usuarioId: String = "uid-1",
        updatedAt: Long = 100L,
        syncState: SyncState = SyncState.SYNCED
    ) = Cliente(
        id = id,
        remoteId = remoteId,
        usuarioId = usuarioId,
        nombre = "Ana",
        createdAt = 1L,
        updatedAt = updatedAt,
        syncState = syncState
    )

    private fun venta(
        id: String = "venta-local-1",
        remoteId: Long? = 30L,
        usuarioId: String = "uid-1",
        updatedAt: Long = 100L,
        syncState: SyncState = SyncState.SYNCED
    ) = Venta(
        id = id,
        remoteId = remoteId,
        usuarioId = usuarioId,
        fecha = 1L,
        updatedAt = updatedAt,
        syncState = syncState
    )

    // ------------------------------------------------------------- productos

    @Test
    fun `si no existe copia local se adopta el remoto sincronizado`() {
        val remoto = producto(remoteId = 99L, updatedAt = 500L)

        val resultado = SyncPolicy.resolverProducto(null, remoto)

        assertEquals(SyncState.SYNCED, resultado?.syncState)
        assertEquals(99L, resultado?.remoteId)
        assertEquals(500L, resultado?.updatedAt)
    }

    @Test
    fun `gana el remoto cuando es mas reciente que el local sincronizado`() {
        val local = producto(updatedAt = 100L, nombre = "Local")
        val remoto = producto(updatedAt = 500L, nombre = "Remoto")

        val resultado = SyncPolicy.resolverProducto(local, remoto)

        assertEquals("Remoto", resultado?.nombre)
        assertEquals(SyncState.SYNCED, resultado?.syncState)
    }

    @Test
    fun `al adoptar el remoto se conserva la pk local y su createdAt`() {
        val local = producto(id = "uuid-local", updatedAt = 100L)
        val remoto = producto(id = "uuid-remoto", remoteId = 99L, updatedAt = 500L)

        val resultado = SyncPolicy.resolverProducto(local, remoto)

        assertEquals("uuid-local", resultado?.id)
        assertEquals(99L, resultado?.remoteId)
        assertEquals(1L, resultado?.createdAt)
    }

    @Test
    fun `gana el local cuando el remoto es mas antiguo o igual`() {
        val local = producto(updatedAt = 900L)
        assertNull(SyncPolicy.resolverProducto(local, producto(updatedAt = 500L)))
        assertNull(SyncPolicy.resolverProducto(local, producto(updatedAt = 900L)))
    }

    @Test
    fun `el local pendiente gana aunque el remoto sea mucho mas nuevo`() {
        val local = producto(updatedAt = 100L, syncState = SyncState.PENDING)
        val remoto = producto(updatedAt = 99_999L)

        assertNull(SyncPolicy.resolverProducto(local, remoto))
    }

    @Test
    fun `el local fallido tambien gana para reintentar el push`() {
        val local = producto(updatedAt = 100L, syncState = SyncState.FAILED)
        val remoto = producto(updatedAt = 99_999L)

        assertNull(SyncPolicy.resolverProducto(local, remoto))
    }

    @Test
    fun `nunca se mezclan productos de usuarios distintos`() {
        val local = producto(usuarioId = "uid-1")
        val remoto = producto(usuarioId = "otro-uid", updatedAt = 99_999L)

        assertNull(SyncPolicy.resolverProducto(local, remoto))
    }

    // -------------------------------------------------------------- clientes

    @Test
    fun `el cliente remoto mas reciente sobrescribe al sincronizado`() {
        val local = cliente(updatedAt = 100L)
        val remoto = cliente(updatedAt = 500L)

        assertEquals(500L, SyncPolicy.resolverCliente(local, remoto)?.updatedAt)
    }

    @Test
    fun `el cliente local pendiente no se sobrescribe`() {
        val local = cliente(updatedAt = 100L, syncState = SyncState.PENDING)
        val remoto = cliente(updatedAt = 99_999L)

        assertNull(SyncPolicy.resolverCliente(local, remoto))
    }

    @Test
    fun `no se adopta un cliente de otra cuenta`() {
        val local = cliente(usuarioId = "uid-1")
        val remoto = cliente(usuarioId = "otro-uid", updatedAt = 99_999L)

        assertNull(SyncPolicy.resolverCliente(local, remoto))
    }

    // ---------------------------------------------------------------- ventas

    @Test
    fun `la venta remota mas reciente sobrescribe a la sincronizada`() {
        val local = venta(updatedAt = 100L)
        val remoto = venta(updatedAt = 500L)

        assertEquals(500L, SyncPolicy.resolverVenta(local, remoto)?.updatedAt)
    }

    @Test
    fun `la venta local pendiente no se sobrescribe`() {
        val local = venta(updatedAt = 100L, syncState = SyncState.PENDING)
        val remoto = venta(updatedAt = 99_999L)

        assertNull(SyncPolicy.resolverVenta(local, remoto))
    }

    @Test
    fun `no se adopta una venta de otra cuenta`() {
        val local = venta(usuarioId = "uid-1")
        val remoto = venta(usuarioId = "otro-uid", updatedAt = 99_999L)

        assertNull(SyncPolicy.resolverVenta(local, remoto))
    }
}
