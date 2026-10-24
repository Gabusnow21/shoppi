package com.shoppi.dto.inventario;

import com.shoppi.entity.TipoMovimiento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de {@code POST /api/inventario/movimiento}.
 */
public record MovimientoRequest(

        @NotNull(message = "es obligatorio")
        Long productoId,

        @NotNull(message = "es obligatorio")
        TipoMovimiento tipoMovimiento,

        @NotNull(message = "es obligatoria")
        @Min(value = 1, message = "debe ser mayor que 0")
        Integer cantidad,

        String descripcion
) {
}
