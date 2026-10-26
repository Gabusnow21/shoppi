package com.shoppi.data.model

/**
 * Tipo de movimiento de inventario, igual que el enum del backend.
 *
 * `ENTRADA` aumenta el stock y `SALIDA` lo descuenta (ajuste manual, merma,
 * rotura, etc.). Las ventas generan sus propios movimientos en el servidor.
 */
enum class TipoMovimiento {
    ENTRADA,
    SALIDA;

    /** Signo con el que afecta el stock: +1 para entrada, -1 para salida. */
    val signo: Int
        get() = if (this == ENTRADA) 1 else -1
}
