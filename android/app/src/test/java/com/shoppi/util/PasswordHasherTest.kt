package com.shoppi.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del hash de contraseñas (SHA-256 + salt). */
class PasswordHasherTest {

    @Test
    fun `la misma contrasena con la misma salt produce el mismo hash`() {
        val salt = "f9a1c2d3e4b567890a1b2c3d4e5f6789"
        assertEquals(
            PasswordHasher.hash("abc12345", salt),
            PasswordHasher.hash("abc12345", salt)
        )
    }

    @Test
    fun `la misma contrasena con distinta salt produce distinto hash`() {
        val hash1 = PasswordHasher.hash("abc12345", PasswordHasher.nuevaSalt())
        val hash2 = PasswordHasher.hash("abc12345", PasswordHasher.nuevaSalt())
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun `el hash es hexadecimal de 64 caracteres`() {
        val hash = PasswordHasher.hash("abc12345", PasswordHasher.nuevaSalt())
        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `el hash no contiene la contrasena en claro`() {
        val password = "clave secreta 123"
        val hash = PasswordHasher.hash(password, PasswordHasher.nuevaSalt())
        assertFalse(hash.contains(password, ignoreCase = true))
    }

    @Test
    fun `verificar valida la contrasena correcta y rechaza la incorrecta`() {
        val salt = PasswordHasher.nuevaSalt()
        val hash = PasswordHasher.hash("abc12345", salt)

        assertTrue(PasswordHasher.verificar("abc12345", salt, hash))
        assertFalse(PasswordHasher.verificar("otraClave1", salt, hash))
    }

    @Test
    fun `las salts generadas no se repiten`() {
        assertNotEquals(PasswordHasher.nuevaSalt(), PasswordHasher.nuevaSalt())
    }
}
