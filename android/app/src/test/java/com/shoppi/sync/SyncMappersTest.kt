package com.shoppi.sync

import com.shoppi.data.model.Cliente
import com.shoppi.data.model.MetodoPago
import com.shoppi.data.model.Producto
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.Venta
import com.shoppi.data.model.VentaItem
import com.shoppi.data.remote.dto.ClienteResponse
import com.shoppi.data.remote.dto.ProductoResponse
import com.shoppi.data.remote.dto.VentaItemResponse
import com.shoppi.data.remote.dto.VentaResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de los mapeos local ⇄ DTO, sin Android ni Retrofit. */
class SyncMappersTest {

    // ------------------------------------------------------------- productos

    @Test
    fun `el producto local se convierte al request de la API`() {
        val producto = Producto(
            id = "local-1",
            usuarioId = "uid-1",
            nombre = "Arroz",
            descripcion = "Arroz premium",
            precio = 1.5,
            stockActual = 10,
            stockMinimo = 2,
            categoria = "Granos",
            imagenUrl = "https://firebasestorage.googleapis.com/x.jpg",
            createdAt = 1L,
            updatedAt = 1L
        )

        val request = producto.toRequest()

        assertEquals("Arroz", request.nombre)
        assertEquals(1.5, request.precio, 0.001)
        assertEquals(10, request.stockActual)
        assertEquals(2, request.stockMinimo)
        assertEquals("https://firebasestorage.googleapis.com/x.jpg", request.imagenUrl)
    }

    @Test
    fun `los campos de texto vacios se envian como nulos`() {
        val producto = Producto(
            id = "local-1",
            usuarioId = "uid-1",
            nombre = "Arroz",
            descripcion = "",
            precio = 1.0,
            categoria = "",
            createdAt = 1L,
            updatedAt = 1L
        )

        val request = producto.toRequest()

        assertNull(request.descripcion)
        assertNull(request.categoria)
        assertNull(request.imagenUrl)
    }

    @Test
    fun `la respuesta de producto se convierte a entidad local sincronizada`() {
        val respuesta = ProductoResponse(
            id = 7L,
            nombre = "Arroz",
            descripcion = null,
            precio = 1.5,
            stockActual = 10,
            stockMinimo = 2,
            categoria = null,
            imagenUrl = null
        )

        val local = respuesta.toLocal(
            usuarioId = "uid-1",
            idLocal = "uuid-local",
            ahora = 500L
        )

        assertEquals("uuid-local", local.id)
        assertEquals(7L, local.remoteId)
        assertEquals("uid-1", local.usuarioId)
        assertEquals(SyncState.SYNCED, local.syncState)
        assertEquals(500L, local.updatedAt)
        assertEquals("", local.descripcion)
        assertEquals("", local.categoria)
    }

    // -------------------------------------------------------------- clientes

    @Test
    fun `la respuesta de cliente se convierte a entidad local sincronizada`() {
        val local = ClienteResponse(
            id = 9L,
            nombre = "Ana",
            telefono = null,
            email = "ana@correo.com",
            direccion = null
        ).toLocal(usuarioId = "uid-1", idLocal = "cli-local", ahora = 700L)

        assertEquals(9L, local.remoteId)
        assertEquals("Ana", local.nombre)
        assertEquals("", local.telefono)
        assertEquals("ana@correo.com", local.email)
        assertEquals(SyncState.SYNCED, local.syncState)
    }

    @Test
    fun `el cliente local se convierte al request con opcionales nulos`() {
        val request = Cliente(
            id = "cli-local",
            usuarioId = "uid-1",
            nombre = "Ana",
            telefono = "",
            email = " ",
            direccion = "",
            createdAt = 1L,
            updatedAt = 1L
        ).toRequest()

        assertEquals("Ana", request.nombre)
        assertNull(request.telefono)
        assertNull(request.email)
        assertNull(request.direccion)
    }

    // ---------------------------------------------------------------- ventas

    private fun ventaLocal() = Venta(
        id = "venta-local-1",
        usuarioId = "uid-1",
        clienteId = "cli-local-1",
        clienteNombre = "Ana",
        items = listOf(
            VentaItem("prod-local-1", "Arroz", 2, 1.5),
            VentaItem("prod-local-2", "Gorra", 1, 7.3)
        ),
        subtotal = 10.3,
        impuestos = 1.34,
        total = 11.64,
        metodoPago = MetodoPago.TARJETA,
        notas = "Entregar mañana",
        fecha = 100L,
        updatedAt = 100L
    )

    @Test
    fun `la venta local se convierte al request resolviendo ids remotos`() {
        val mapa = mapOf("prod-local-1" to 11L, "prod-local-2" to 22L)

        val request = ventaLocal().toRequest(
            clienteRemoteId = 33L,
            productoRemoteId = { local -> local?.let { mapa[it] } }
        )

        assertEquals(33L, request.clienteId)
        assertEquals("TARJETA", request.metodoPago)
        assertEquals("Entregar mañana", request.notas)
        assertEquals(2, request.detalles.size)
        assertEquals(11L, request.detalles[0].productoId)
        assertEquals(2, request.detalles[0].cantidad)
        assertEquals(22L, request.detalles[1].productoId)
    }

    @Test
    fun `una venta con producto sin id remoto falla explicitamente`() {
        assertThrows(IllegalStateException::class.java) {
            ventaLocal().toRequest(
                clienteRemoteId = null,
                productoRemoteId = { null }
            )
        }
    }

    @Test
    fun `la respuesta de venta se convierte a entidad local`() {
        val respuesta = VentaResponse(
            id = 50L,
            fecha = "2026-08-23T10:30:00",
            subtotal = 10.3,
            impuestos = 1.34,
            total = 11.64,
            metodoPago = "TARJETA",
            clienteId = 33L,
            detalles = listOf(
                VentaItemResponse(11L, 2, 1.5, 3.0),
                VentaItemResponse(99L, 1, 7.3, 7.3)
            )
        )

        val venta = respuesta.toLocal(
            usuarioId = "uid-1",
            idLocal = "venta-local",
            clienteLocal = { id ->
                if (id == 33L) ClienteRemotoLocal("cli-local", "Ana") else null
            },
            productoLocal = { id ->
                if (id == 11L) ProductoRemotoLocal("prod-local", "Arroz") else null
            },
            ahora = 999L
        )

        assertEquals("venta-local", venta.id)
        assertEquals(50L, venta.remoteId)
        assertEquals(MetodoPago.TARJETA, venta.metodoPago)
        assertEquals("cli-local", venta.clienteId)
        assertEquals("Ana", venta.clienteNombre)
        assertEquals(2, venta.items.size)
        assertEquals("prod-local", venta.items[0].productoId)
        assertEquals("Arroz", venta.items[0].productoNombre)
        assertNull(venta.items[1].productoId)
        assertEquals("", venta.items[1].productoNombre)
        assertEquals(999L, venta.updatedAt)
        assertTrue(venta.fecha > 0L)
        assertEquals(SyncState.SYNCED, venta.syncState)
    }

    @Test
    fun `una fecha ausente usa el instante de sincronizacion`() {
        val venta = VentaResponse(id = 51L, fecha = null).toLocal(
            usuarioId = "uid-1",
            ahora = 1234L
        )

        assertEquals(1234L, venta.fecha)
    }

    @Test
    fun `un metodo de pago desconocido cae en efectivo`() {
        val venta = VentaResponse(id = 52L, metodoPago = "CRIPTO").toLocal(
            usuarioId = "uid-1",
            ahora = 1L
        )

        assertEquals(MetodoPago.EFECTIVO, venta.metodoPago)
    }
}
