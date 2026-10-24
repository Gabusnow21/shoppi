package com.shoppi.dto.reporte;

import java.math.BigDecimal;

/**
 * Estado de un producto dentro del reporte de inventario.
 */
public record ProductoInventarioResponse(
        Long productoId,
        String nombre,
        int stockActual,
        int stockMinimo,
        BigDecimal precio,
        BigDecimal valorStock,
        boolean stockBajo
) {
}
