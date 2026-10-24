package com.shoppi.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo de {@code POST /api/clientes} y {@code PUT /api/clientes/{id}}.
 *
 * <p>Teléfono, correo y dirección son opcionales; si llegan en blanco se
 * guardan como {@code null}.</p>
 */
public record ClienteRequest(

        @NotBlank(message = "es obligatorio")
        @Size(max = 150, message = "no debe superar 150 caracteres")
        String nombre,

        @Size(max = 20, message = "no debe superar 20 caracteres")
        String telefono,

        @Email(message = "no tiene un formato válido")
        @Size(max = 150, message = "no debe superar 150 caracteres")
        String email,

        String direccion
) {
}
