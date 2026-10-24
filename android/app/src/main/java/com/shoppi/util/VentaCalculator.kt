package com.shoppi.util

import com.shoppi.data.model.VentaItem
import kotlin.math.round

/**
 * Cálculo de totales de la venta.
 *
 * Objeto puro (sin Android) para probarlo con JUnit. Se aplica el IVA de
 * El Salvador (13 %) sobre el subtotal ya redondeado.
 */
object VentaCalculator {

    const val TASA_IMPUESTO = 0.13

    /** Suma de los importes de cada línea. */
    fun subtotal(items: List<VentaItem>): Double =
        redondear(items.sumOf { it.importe })

    /** Impuesto sobre el subtotal redondeado. */
    fun impuesto(subtotal: Double): Double = redondear(subtotal * TASA_IMPUESTO)

    /** Subtotal + impuesto. */
    fun total(subtotal: Double, impuesto: Double): Double = redondear(subtotal + impuesto)

    /** Calcula los tres valores a partir de las líneas. */
    fun calcularTotales(items: List<VentaItem>): Totales {
        val sub = subtotal(items)
        val imp = impuesto(sub)
        return Totales(subtotal = sub, impuesto = imp, total = total(sub, imp))
    }

    /** Redondeo comercial a 2 decimales (moneda). */
    fun redondear(valor: Double): Double = round(valor * 100) / 100

    data class Totales(
        val subtotal: Double,
        val impuesto: Double,
        val total: Double
    )
}
