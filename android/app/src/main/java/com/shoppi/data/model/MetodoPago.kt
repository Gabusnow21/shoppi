package com.shoppi.data.model

/** Métodos de pago aceptados por el punto de venta (solo se registran). */
enum class MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA;

    companion object {
        fun fromStorage(valor: String?): MetodoPago =
            entries.firstOrNull { it.name == valor } ?: EFECTIVO
    }
}
