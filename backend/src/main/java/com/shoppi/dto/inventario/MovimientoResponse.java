package com.shoppi.dto.inventario;

import com.shoppi.entity.TipoMovimiento;

import java.time.LocalDateTime;

/**
 * Movimiento de inventario devuelto por la API.
 */
public record MovimientoResponse(
        Long id,
        Long productoId,
        String productoNombre,
        TipoMovimiento tipoMovimiento,
        int cantidad,
        LocalDateTime fecha,
        String descripcion
) {
}
