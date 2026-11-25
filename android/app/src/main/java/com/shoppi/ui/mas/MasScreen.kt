package com.shoppi.ui.mas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoppi.ui.theme.Dimens

/**
 * Hub "Más": agrupa las secciones que no son destino principal de la barra
 * inferior (ventas, inventario y reportes).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasScreen(
    onVentas: () -> Unit,
    onInventario: () -> Unit,
    onReportes: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Más") }) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = padding.calculateTopPadding() + Dimens.InputPadding,
                bottom = Dimens.ScreenPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                OpcionMas(
                    icono = Icons.Filled.ReceiptLong,
                    titulo = "Ventas",
                    descripcion = "Historial de ventas y tickets",
                    onClick = onVentas,
                )
            }
            item {
                OpcionMas(
                    icono = Icons.Filled.Inventory,
                    titulo = "Inventario",
                    descripcion = "Movimientos de entradas y salidas",
                    onClick = onInventario,
                )
            }
            item {
                OpcionMas(
                    icono = Icons.Filled.BarChart,
                    titulo = "Reportes",
                    descripcion = "Ventas por período y stock",
                    onClick = onReportes,
                )
            }
        }
    }
}

@Composable
private fun OpcionMas(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Dimens.InputPadding))
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
