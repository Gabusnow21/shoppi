package com.shoppi.ui.ventas

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoppi.ShoppiApp
import com.shoppi.data.model.Venta
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.components.SyncStateBadge
import com.shoppi.ui.theme.Dimens
import com.shoppi.util.Formatos

/**
 * Historial de ventas del usuario.
 *
 * Cada fila abre el ticket correspondiente. Las ventas pendientes de
 * sincronizar no tienen id remoto todavía, así que no se pueden abrir hasta
 * que el `SyncManager` las empuje.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaListScreen(
    onVolver: () -> Unit,
    onAbrirTicket: (Long) -> Unit,
    viewModel: VentaListViewModel = viewModel(factory = ventaListViewModelFactory()),
) {
    val ventas by viewModel.ventas.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ventas") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (ventas.isEmpty()) {
            EmptyState(
                titulo = "Todavía no hay ventas",
                mensaje = "Registrá tu primera venta desde Vender.",
                icono = Icons.Filled.ReceiptLong,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
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
                items(ventas, key = { it.id }) { venta ->
                    VentaCard(
                        venta = venta,
                        onClick = {
                            venta.remoteId?.let(onAbrirTicket)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VentaCard(
    venta: Venta,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = venta.remoteId != null,
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Venta #${venta.remoteId ?: "local"}",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = buildString {
                        append(Formatos.fecha(venta.fecha))
                        append(" · ")
                        append(venta.metodoPago.name.lowercase().replaceFirstChar { it.uppercase() })
                        if (venta.clienteNombre.isNotBlank()) {
                            append(" · ")
                            append(venta.clienteNombre)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SyncStateBadge(venta.syncState)
            }

            Spacer(Modifier.width(Dimens.InputPadding))

            Text(
                text = Formatos.moneda(venta.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ventaListViewModelFactory(): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container) { VentaListViewModel.factory(container.ventas) }
}
