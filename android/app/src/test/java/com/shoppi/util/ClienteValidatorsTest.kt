package com.shoppi.util

import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de las validaciones de cliente. */
class ClienteValidatorsTest {

    @Test
    fun `un cliente con solo nombre es valido`() {
        assertTrue(ClienteValidators.errores("Ana", "", "").isEmpty())
    }

    @Test
    fun `un cliente completo y bien formado es valido`() {
        assertTrue(
            ClienteValidators.errores(
                nombre = "Ana María López",
                telefono = "7777-7777",
                email = "ana@correo.com"
            ).isEmpty()
        )
    }

    @Test
    fun `el nombre demasiado corto se rechaza`() {
        assertTrue(
            ClienteValidators.errores("Al", "", "").contains(ErrorCliente.NOMBRE_CORTO)
        )
    }

    @Test
    fun `un email con formato invalido se rechaza`() {
        assertTrue(
            ClienteValidators.errores("Ana", "", "ana-correo")
                .contains(ErrorCliente.EMAIL_INVALIDO)
        )
    }

    @Test
    fun `un telefono con letras se rechaza`() {
        assertTrue(
            ClienteValidators.errores("Ana", "llamame", "")
                .contains(ErrorCliente.TELEFONO_INVALIDO)
        )
    }

    @Test
    fun `un telefono corto se rechaza`() {
        assertTrue(
            ClienteValidators.errores("Ana", "123", "")
                .contains(ErrorCliente.TELEFONO_INVALIDO)
        )
    }
}
