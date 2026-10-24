package com.shoppi.dto.venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Venta devuelta por la API.
 *
 * <p>Coincide con {@code VentaResponse} del cliente Android: {@code fecha} se
 * serializa como ISO-8601 y {@code clienteId} puede ser {@code null}.</p>
 */
public record VentaResponse(
        Long id,
        LocalDateTime fecha,
        BigDecimal subtotal,
        BigDecimal impuestos,
        BigDecimal total,
        String metodoPago,
        Long clienteId,
        List<DetalleVentaResponse> detalles
) {
}
