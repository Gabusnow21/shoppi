package com.shoppi.dto.venta;

import java.math.BigDecimal;

/**
 * Línea devuelta dentro del detalle de una venta.
 */
public record DetalleVentaResponse(
        Long productoId,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
