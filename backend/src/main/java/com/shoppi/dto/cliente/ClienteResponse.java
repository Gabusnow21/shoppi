package com.shoppi.dto.cliente;

/**
 * Cliente devuelto por la API.
 *
 * <p>Coincide con {@code ClienteResponse} del cliente Android y con
 * {@code docs/api_specification.md}.</p>
 */
public record ClienteResponse(
        Long id,
        String nombre,
        String telefono,
        String email,
        String direccion
) {
}
