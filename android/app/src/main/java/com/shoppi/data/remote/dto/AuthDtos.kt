package com.shoppi.data.remote.dto

/** Cuerpo de `POST /api/auth/register`. */
data class RegisterRequest(
    val nombre: String,
    val email: String,
    val password: String,
    val telefono: String? = null
)

/** Cuerpo de `POST /api/auth/login`. */
data class LoginRequest(
    val email: String,
    val password: String
)

/** Respuesta común de registro y login. */
data class AuthResponse(
    val id: Long,
    val nombre: String = "",
    val email: String = "",
    val token: String = ""
)
