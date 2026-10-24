package com.shoppi.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ventas acumuladas de un día.
 */
public record VentaPorDiaResponse(
        LocalDate fecha,
        long cantidad,
        BigDecimal monto
) {
}
