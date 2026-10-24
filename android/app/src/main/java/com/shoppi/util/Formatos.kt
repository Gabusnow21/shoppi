package com.shoppi.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formateo de valores para la UI: moneda y fechas.
 *
 * El comercio opera en dólares, así que la moneda se imprime con `Locale.US`
 * para obtener siempre `$1234.56` sin importar la configuración regional.
 */
object Formatos {

    fun moneda(valor: Double): String = String.format(Locale.US, "$%.2f", valor)

    fun fecha(epochMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(epochMillis))

    fun fechaCorta(epochMillis: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(epochMillis))
}
