package com.shoppi.data.model

/**
 * Línea de una venta.
 *
 * No es una entidad Room: las líneas se serializan como JSON en la columna
 * `itemsJson` de [Venta]. Así la venta se escribe de forma atómica y no depende
 * de que los productos ya tengan `remoteId` para poder guardarse sin conexión.
 */
data class VentaItem(
    /** Id local del producto (null si la venta vino de la API sin poder mapearlo). */
    val productoId: String? = null,
    val productoNombre: String,
    val cantidad: Int,
    val precioUnitario: Double
) {
    /** Importe de la línea: cantidad × precio unitario. */
    val importe: Double
        get() = cantidad * precioUnitario
}
