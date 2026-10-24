package com.shoppi.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Nivel de stock derivado del mínimo configurado. */
enum class NivelStock { AGOTADO, BAJO, DISPONIBLE }

fun nivelStock(stockActual: Int, stockMinimo: Int): NivelStock = when {
    stockActual <= 0 -> NivelStock.AGOTADO
    stockActual <= stockMinimo -> NivelStock.BAJO
    else -> NivelStock.DISPONIBLE
}

/**
 * Badge de stock: rojo agotado, amarillo bajo, verde disponible.
 * El color nunca se hardcodea en la pantalla; sale del `colorScheme`.
 */
@Composable
fun StockBadge(
    stockActual: Int,
    stockMinimo: Int,
    modifier: Modifier = Modifier,
) {
    val nivel = nivelStock(stockActual, stockMinimo)
    val color: Color = when (nivel) {
        NivelStock.AGOTADO -> MaterialTheme.colorScheme.error
        NivelStock.BAJO -> MaterialTheme.colorScheme.tertiary
        NivelStock.DISPONIBLE -> MaterialTheme.colorScheme.primary
    }
    val texto = when (nivel) {
        NivelStock.AGOTADO -> "Sin stock"
        NivelStock.BAJO -> "Stock bajo ($stockActual)"
        NivelStock.DISPONIBLE -> "Stock: $stockActual"
    }

    Surface(
        color = color.copy(alpha = 0.16f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
