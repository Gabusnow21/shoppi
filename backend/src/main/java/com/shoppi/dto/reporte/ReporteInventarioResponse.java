package com.shoppi.dto.reporte;

import java.math.BigDecimal;
import java.util.List;

/**
 * Reporte de estado del inventario.
 */
public record ReporteInventarioResponse(
        int totalProductos,
        int productosStockBajo,
        BigDecimal valorInventario,
        List<ProductoInventarioResponse> productos
) {
}
