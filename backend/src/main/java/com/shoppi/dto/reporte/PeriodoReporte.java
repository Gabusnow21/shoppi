package com.shoppi.dto.reporte;

import java.util.Locale;

/**
 * Período del reporte de ventas.
 */
public enum PeriodoReporte {
    DIARIO,
    SEMANAL,
    MENSUAL;

    /**
     * Convierte el valor recibido por query param sin distinguir mayúsculas.
     * Un valor desconocido se traduce a 400 en el manejador global.
     */
    public static PeriodoReporte desde(String valor) {
        if (valor == null || valor.isBlank()) {
            return DIARIO;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "periodo debe ser diario, semanal o mensual.");
        }
    }

    /** Nombre en minúsculas usado en el JSON de respuesta. */
    public String etiqueta() {
        return name().toLowerCase(Locale.ROOT);
    }
}
