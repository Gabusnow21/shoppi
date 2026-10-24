package com.shoppi.dto.producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code POST /api/productos} y {@code PUT /api/productos/{id}}.
 *
 * <p>{@code stockActual} y {@code stockMinimo} son opcionales al crear: si se
 * omiten se aplican los valores por defecto de la base (0 y 5).</p>
 */
public record ProductoRequest(

        @NotBlank(message = "es obligatorio")
        @Size(max = 150, message = "no debe superar 150 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "es obligatorio")
        @DecimalMin(value = "0.00", message = "debe ser mayor o igual a 0")
        @Digits(integer = 8, fraction = 2, message = "debe tener hasta 8 enteros y 2 decimales")
        BigDecimal precio,

        @Min(value = 0, message = "no puede ser negativo")
        Integer stockActual,

        @Min(value = 0, message = "no puede ser negativo")
        Integer stockMinimo,

        @Size(max = 100, message = "no debe superar 100 caracteres")
        String categoria,

        @Size(max = 500, message = "no debe superar 500 caracteres")
        @URL(message = "debe ser una URL válida")
        String imagenUrl
) {
}
