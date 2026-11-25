package com.shoppi.ui.ventas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoppi.data.model.MetodoPago
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.Venta
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.theme.Dimens
import com.shoppi.util.Formatos

/**
 * Ticket de una venta confirmada (plan_ui_compose.md).
 *
 * Se abre desde el push `VENTA_CONFIRMADA`, que llega con el id remoto de la
 * venta. Muestra el detalle completo: líneas, totales, cliente y método de pago.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    ventaId: Long,
    onVolver: () -> Unit,
    viewModel: TicketViewModel,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket #$ventaId") },
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
        when {
            estado.cargando -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }

            estado.venta == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    titulo = "Ticket no disponible",
                    mensaje = estado.error ?: "No se pudo cargar la venta.",
                    icono = Icons.Filled.Refresh,
                )
                Button(onClick = viewModel::cargar) {
                    Text("Reintentar")
                }
            }

            else -> TicketContenido(
                venta = estado.venta!!,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@Composable
private fun TicketContenido(venta: Venta, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardPadding),
    ) {
        // Encabezado del comprobante.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Shoppi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Venta #${venta.remoteId ?: "local"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = Formatos.fecha(venta.fecha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (venta.syncState == SyncState.SYNCED) {
                        "Sincronizada"
                    } else {
                        "Pendiente de sincronizar"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (venta.syncState == SyncState.SYNCED) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }

        // Datos del cliente y pago.
        FilaDato("Cliente", venta.clienteNombre.ifBlank { "Consumidor final" })
        FilaDato("Método de pago", venta.metodoPago.texto())
        if (venta.notas.isNotBlank()) {
            FilaDato("Notas", venta.notas)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Text(
            text = "Productos",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )

        venta.items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productoNombre,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${item.cantidad} × ${Formatos.moneda(item.precioUnitario)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = Formatos.moneda(item.importe),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        FilaDato("Subtotal", Formatos.moneda(venta.subtotal))
        FilaDato("IVA (13 %)", Formatos.moneda(venta.impuestos))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = Formatos.moneda(venta.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(Dimens.InputPadding))
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

/** Nombre visible del método de pago. */
private fun MetodoPago.texto(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }
