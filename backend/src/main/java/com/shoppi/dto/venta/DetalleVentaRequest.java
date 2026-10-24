package com.shoppi.dto.venta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Línea del cuerpo de {@code POST /api/ventas}.
 */
public record DetalleVentaRequest(

        @NotNull(message = "es obligatorio")
        Long productoId,

        @NotNull(message = "es obligatoria")
        @Min(value = 1, message = "debe ser mayor que 0")
        Integer cantidad,

        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.00", message = "debe ser mayor o igual a 0")
        @Digits(integer = 8, fraction = 2, message = "debe tener hasta 8 enteros y 2 decimales")
        BigDecimal precioUnitario
) {
}
