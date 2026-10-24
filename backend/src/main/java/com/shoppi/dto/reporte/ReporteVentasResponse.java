package com.shoppi.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Reporte de ventas por período.
 *
 * <p>{@code ventasPorDia} siempre se agrupa por día dentro del rango efectivo,
 * incluso cuando el período solicitado es semanal o mensual; así el cliente
 * Android puede dibujar una serie completa sin recalcular nada.</p>
 */
public record ReporteVentasResponse(
        String periodo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        long totalVentas,
        BigDecimal montoTotal,
        List<VentaPorDiaResponse> ventasPorDia
) {
}
