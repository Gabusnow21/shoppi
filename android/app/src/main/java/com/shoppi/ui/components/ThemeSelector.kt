package com.shoppi.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoppi.ui.theme.ThemeMode

/**
 * Selector de apariencia: "Sistema" con texto y los modos forzados con icono
 * (sol para Claro, luna para Oscuro).
 *
 * Reutilizado en el hub "Más" y como acceso rápido en el dashboard.
 */
@Composable
fun ThemeSelector(
    modo: ThemeMode,
    onSeleccionar: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = modo == ThemeMode.SYSTEM,
            onClick = { onSeleccionar(ThemeMode.SYSTEM) },
            label = { Text("Sistema") },
        )
        FilterChip(
            selected = modo == ThemeMode.LIGHT,
            onClick = { onSeleccionar(ThemeMode.LIGHT) },
            label = {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = "Claro",
                )
            },
        )
        FilterChip(
            selected = modo == ThemeMode.DARK,
            onClick = { onSeleccionar(ThemeMode.DARK) },
            label = {
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = "Oscuro",
                )
            },
        )
    }
}
