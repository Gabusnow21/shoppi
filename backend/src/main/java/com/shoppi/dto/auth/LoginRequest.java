package com.shoppi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo de {@code POST /api/auth/login}.
 *
 * <p>No se aplican reglas de formato a la contraseña: una credencial inválida
 * debe responder 401, no 400.</p>
 */
public record LoginRequest(

        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene un formato válido")
        String email,

        @NotBlank(message = "es obligatoria")
        String password
) {
}
