package com.shoppi.dto.producto;

import java.math.BigDecimal;

/**
 * Producto devuelto por la API.
 *
 * <p>Coincide con {@code ProductoResponse} del cliente Android y con
 * {@code docs/api_specification.md}.</p>
 */
public record ProductoResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        int stockActual,
        int stockMinimo,
        String categoria,
        String imagenUrl
) {
}
