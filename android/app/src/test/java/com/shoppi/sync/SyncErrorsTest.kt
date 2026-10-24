package com.shoppi.sync

import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Pruebas de la detección de errores reintentables.
 *
 * Si es de red/5xx, el registro sigue `PENDING`; si es 4xx, se marca `FAILED`.
 */
class SyncErrorsTest {

    private fun http(codigo: Int): HttpException = HttpException(
        Response.error<Any>(
            codigo,
            ResponseBody.create(MediaType.parse("application/json"), "{}")
        )
    )

    @Test
    fun `un IOException se considera error de red`() {
        assertTrue(esErrorDeRed(IOException("sin red")))
        assertTrue(esErrorDeRed(SocketTimeoutException("timeout")))
    }

    @Test
    fun `un error envuelto tambien se detecta`() {
        val envuelto = RuntimeException("fallo", IOException("sin red"))
        assertTrue(esErrorDeRed(envuelto))
    }

    @Test
    fun `los 5xx y limites de peticiones se reintentan`() {
        assertTrue(esErrorDeRed(http(500)))
        assertTrue(esErrorDeRed(http(503)))
        assertTrue(esErrorDeRed(http(429)))
        assertTrue(esErrorDeRed(http(408)))
    }

    @Test
    fun `los errores de validacion o permisos no se reintentan`() {
        assertFalse(esErrorDeRed(http(400)))
        assertFalse(esErrorDeRed(http(401)))
        assertFalse(esErrorDeRed(http(403)))
        assertFalse(esErrorDeRed(http(404)))
        assertFalse(esErrorDeRed(http(422)))
    }

    @Test
    fun `un error normal no es de red`() {
        assertFalse(esErrorDeRed(IllegalStateException("dato invalido")))
        assertFalse(esErrorDeRed(RuntimeException()))
    }
}
