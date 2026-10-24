package com.shoppi.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de las validaciones de autenticación: correo electrónico, contraseña
 * alfanumérica de mínimo 8 caracteres y nombre completo de al menos 7.
 */
class ValidatorsTest {

    @Test
    fun `acepta un email valido`() {
        assertTrue(Validators.emailValido("ana.lopez@correo.com"))
    }

    @Test
    fun `acepta un email con numeros y guiones`() {
        assertTrue(Validators.emailValido("ana-maria-2024@dominio.co.sv"))
    }

    @Test
    fun `rechaza un email sin arroba`() {
        assertFalse(Validators.emailValido("anacorreo.com"))
    }

    @Test
    fun `rechaza un email sin dominio`() {
        assertFalse(Validators.emailValido("ana@"))
    }

    @Test
    fun `rechaza un email con arroba duplicada`() {
        assertFalse(Validators.emailValido("ana@@correo.com"))
    }

    @Test
    fun `rechaza un email vacio o nulo`() {
        assertFalse(Validators.emailValido(""))
        assertFalse(Validators.emailValido("   "))
        assertFalse(Validators.emailValido(null))
    }

    @Test
    fun `acepta una contrasena de 8 caracteres alfanumericos`() {
        assertTrue(Validators.contrasenaValida("abc12345"))
    }

    @Test
    fun `acepta una contrasena mas larga`() {
        assertTrue(Validators.contrasenaValida("shoppi2026ventas"))
    }

    @Test
    fun `rechaza una contrasena de menos de 8 caracteres`() {
        assertFalse(Validators.contrasenaValida("abc1234"))
    }

    @Test
    fun `rechaza una contrasena con simbolos`() {
        assertFalse(Validators.contrasenaValida("abc1234$"))
    }

    @Test
    fun `acepta una contrasena solo de letras`() {
        assertTrue(Validators.contrasenaValida("abcdefghij"))
    }

    @Test
    fun `acepta una contrasena solo de numeros`() {
        assertTrue(Validators.contrasenaValida("12345678"))
    }

    @Test
    fun `rechaza una contrasena con espacios`() {
        assertFalse(Validators.contrasenaValida("abc 12345"))
    }

    @Test
    fun `rechaza una contrasena vacia o nula`() {
        assertFalse(Validators.contrasenaValida(""))
        assertFalse(Validators.contrasenaValida(null))
    }

    @Test
    fun `acepta un nombre de 7 caracteres`() {
        assertTrue(Validators.nombreCompletoValido("Ana Lop"))
    }

    @Test
    fun `acepta un nombre largo con espacios`() {
        assertTrue(Validators.nombreCompletoValido("Ana María López"))
    }

    @Test
    fun `rechaza un nombre de menos de 7 caracteres`() {
        assertFalse(Validators.nombreCompletoValido("Ana L"))
    }

    @Test
    fun `rechaza un nombre vacio o nulo`() {
        assertFalse(Validators.nombreCompletoValido(""))
        assertFalse(Validators.nombreCompletoValido(null))
    }

    @Test
    fun `las contrasenas coinciden`() {
        assertTrue(Validators.contrasenasCoinciden("abc12345", "abc12345"))
    }

    @Test
    fun `las contrasenas no coinciden`() {
        assertFalse(Validators.contrasenasCoinciden("abc12345", "abc12346"))
    }

    @Test
    fun `la confirmacion vacia no coincide`() {
        assertFalse(Validators.contrasenasCoinciden("abc12345", ""))
        assertFalse(Validators.contrasenasCoinciden(null, null))
    }
}
