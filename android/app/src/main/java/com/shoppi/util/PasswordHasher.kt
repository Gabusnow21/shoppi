package com.shoppi.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Hash de contraseñas con SHA-256 + salt aleatorio por usuario.
 *
 * La contraseña en claro nunca se persiste: Room guarda el hash y su salt para
 * permitir el inicio de sesión offline contra la copia local. El backend valida
 * la contraseña real con bcrypt cuando hay conexión.
 */
object PasswordHasher {

    private const val ALGORITMO = "SHA-256"
    private const val TAMANO_SALT_BYTES = 16

    /** Genera una salt aleatoria nueva en hexadecimal. */
    fun nuevaSalt(tamanoBytes: Int = TAMANO_SALT_BYTES): String {
        val bytes = ByteArray(tamanoBytes)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Devuelve el hash hexadecimal de `password` combinado con `salt`. */
    fun hash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance(ALGORITMO)
        val resultado = digest.digest((salt + password).toByteArray(Charsets.UTF_8))
        return resultado.joinToString("") { "%02x".format(it) }
    }

    /** Verifica que `password` coincida con un hash almacenado. */
    fun verificar(password: String, salt: String, hashAlmacenado: String): Boolean =
        hash(password, salt) == hashAlmacenado
}
