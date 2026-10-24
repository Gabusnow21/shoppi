package com.shoppi.dto.auth;

/**
 * Respuesta común de registro e inicio de sesión.
 *
 * <p>El contrato coincide con {@code docs/api_specification.md}: el cliente
 * Android guarda el {@code token} JWT y usa {@code id} como llave del usuario.</p>
 */
public record AuthResponse(
        Long id,
        String nombre,
        String email,
        String token
) {
}
