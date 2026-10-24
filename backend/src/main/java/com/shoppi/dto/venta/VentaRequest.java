package com.shoppi.dto.venta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Cuerpo de {@code POST /api/ventas}.
 *
 * <p>El usuario sale del JWT; {@code clienteId} es opcional. El cliente Android
 * envía además {@code notas}, que el backend ignora porque no forma parte del
 * esquema de ventas.</p>
 */
public record VentaRequest(

        Long clienteId,

        @Size(max = 50, message = "no debe superar 50 caracteres")
        String metodoPago,

        @NotEmpty(message = "debe incluir al menos un producto")
        @Valid
        List<DetalleVentaRequest> detalles
) {
}
