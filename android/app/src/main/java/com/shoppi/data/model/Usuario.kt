package com.shoppi.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Usuario local.
 *
 * La contraseña en claro nunca se guarda: se almacena
 * `SHA-256(salt + password)` y su salt para permitir el inicio de sesión
 * offline contra Room cuando la API no responde. La sesión activa (JWT) vive en
 * [com.shoppi.util.SessionManager], nunca en esta tabla.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["email"], unique = true)]
)
data class Usuario(
    /** Id del servidor convertido a texto (el JWT usa un id numérico). */
    @PrimaryKey
    val id: String,
    val email: String,
    val nombreCompleto: String,
    val telefono: String = "",
    val passwordHash: String,
    val salt: String,
    val createdAt: Long,
    val updatedAt: Long
)
