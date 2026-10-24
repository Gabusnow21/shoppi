package com.shoppi.data.local

import com.shoppi.data.model.MetodoPago
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.VentaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de los TypeConverters de Room, en especial el JSON de `itemsJson`
 * (las líneas de venta viajan como JSON para que la escritura sea atómica).
 */
class ConvertersTest {

    private val converters = Converters()

    private val items = listOf(
        VentaItem("prod-1", "Camiseta deportiva", 2, 12.50),
        VentaItem("prod-2", "Gorra", 1, 7.30)
    )

    @Test
    fun `la lista de items sobrevive al ida y vuelta por JSON`() {
        val json = converters.deItems(items)
        val recuperado = converters.aItems(json)

        assertEquals(items, recuperado)
    }

    @Test
    fun `el JSON generado contiene los campos esperados`() {
        val json = converters.deItems(items)

        assertTrue(json.contains("Camiseta deportiva"))
        assertTrue(json.contains("productoNombre"))
        assertTrue(json.contains("cantidad"))
        assertTrue(json.contains("precioUnitario"))
    }

    @Test
    fun `json vacio o nulo devuelve una lista vacia`() {
        assertTrue(converters.aItems(null).isEmpty())
        assertTrue(converters.aItems("").isEmpty())
        assertTrue(converters.aItems("   ").isEmpty())
    }

    @Test
    fun `json corrupto no revienta y devuelve una lista vacia`() {
        assertTrue(converters.aItems("{esto no es json}").isEmpty())
    }

    @Test
    fun `lista vacia se serializa y deserializa correctamente`() {
        assertEquals("[]", converters.deItems(emptyList()))
        assertTrue(converters.aItems(converters.deItems(emptyList())).isEmpty())
    }

    @Test
    fun `el metodo de pago sobrevive al ida y vuelta`() {
        MetodoPago.entries.forEach { metodo ->
            assertEquals(metodo, converters.aMetodoPago(converters.deMetodoPago(metodo)))
        }
    }

    @Test
    fun `el syncState sobrevive al ida y vuelta`() {
        SyncState.entries.forEach { estado ->
            assertEquals(estado, converters.aSyncState(converters.deSyncState(estado)))
        }
    }

    @Test
    fun `valores desconocidos caen en el estado por defecto`() {
        assertEquals(MetodoPago.EFECTIVO, converters.aMetodoPago("CRYPTO"))
        assertEquals(SyncState.PENDING, converters.aSyncState("DESCONOCIDO"))
        assertEquals(SyncState.PENDING, converters.aSyncState(null))
    }
}
