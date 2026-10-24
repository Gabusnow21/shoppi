package com.shoppi.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/auth/register}.
 *
 * <p>Las reglas replican las del cliente Android: nombre completo de al menos 7
 * caracteres, correo con formato válido y contraseña alfanumérica de mínimo 8
 * caracteres. La contraseña admite letras Unicode (por ejemplo {@code ñ}).</p>
 */
public record RegisterRequest(

        @NotBlank(message = "es obligatorio")
        @Size(min = 7, max = 100, message = "debe tener entre 7 y 100 caracteres")
        String nombre,

        @NotBlank(message = "es obligatorio")
        @Email(message = "no tiene un formato válido")
        @Size(max = 150, message = "no debe superar 150 caracteres")
        String email,

        @NotBlank(message = "es obligatoria")
        @Size(min = 8, max = 72, message = "debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N}]+$", message = "debe ser alfanumérica")
        String password,

        @Size(max = 20, message = "no debe superar 20 caracteres")
        String telefono
) {
}
