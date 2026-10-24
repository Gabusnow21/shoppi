package com.shoppi.dto.reporte;

import java.math.BigDecimal;

/**
 * Producto dentro del top de más vendidos.
 *
 * <p>{@code montoTotal} es la suma de los subtotales de las líneas vendidas,
 * sin impuestos.</p>
 */
public record ProductoMasVendidoResponse(
        Long productoId,
        String nombre,
        long cantidadVendida,
        BigDecimal montoTotal
) {
}
