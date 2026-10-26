package com.shoppi.ui.ventas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoppi.data.model.MetodoPago
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.theme.Dimens
import com.shoppi.util.Formatos

/**
 * Resumen del carrito y confirmación de la venta (plan_ui_compose.md P7).
 *
 * Permite ajustar cantidades, elegir cliente (opcional) y método de pago, y
 * muestra subtotal + IVA 13 % antes de confirmar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarritoScreen(
    onConfirmado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: VentaViewModel,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    LaunchedEffect(estado.mensajeExito) {
        // El snackbar lo muestra PuntoVentaScreen al volver; aquí solo navegamos.
        if (estado.mensajeExito != null) onConfirmado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carrito") },
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
        if (estado.lineas.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    titulo = "El carrito está vacío",
                    mensaje = "Agrega productos desde el punto de venta.",
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
            ) {
                estado.lineas.forEach { linea ->
                    LineaCarritoRow(
                        linea = linea,
                        onIncrementar = { viewModel.incrementar(linea.productoId) },
                        onDecrementar = { viewModel.decrementar(linea.productoId) },
                        onQuitar = { viewModel.quitar(linea.productoId) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }

                ClienteSelector(
                    clientes = estado.clientes.map { it.id to it.nombre },
                    clienteId = estado.clienteId,
                    onSeleccionar = viewModel::seleccionarCliente,
                )

                Text(
                    text = "Método de pago",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetodoPago.entries.forEach { metodo ->
                        FilterChip(
                            selected = estado.metodoPago == metodo,
                            onClick = { viewModel.cambiarMetodoPago(metodo) },
                            label = { Text(metodo.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }

                TotalesBlock(subtotal = estado.totales.subtotal, impuesto = estado.totales.impuesto)

                if (estado.error != null) {
                    Text(
                        text = estado.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = viewModel::confirmar,
                    enabled = !estado.cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.ButtonHeight),
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Confirmar venta · ${Formatos.moneda(estado.totales.total)}")
                    }
                }

                Text(
                    text = "La venta se guarda en el dispositivo y se sincroniza " +
                        "automáticamente; el stock lo descuenta el servidor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun LineaCarritoRow(
    linea: LineaCarrito,
    onIncrementar: () -> Unit,
    onDecrementar: () -> Unit,
    onQuitar: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = linea.productoNombre,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = "${Formatos.moneda(linea.precioUnitario)} c/u",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onDecrementar) {
            Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Quitar una unidad de ${linea.productoNombre}",
            )
        }
        Text(
            text = linea.cantidad.toString(),
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = onIncrementar) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Agregar una unidad de ${linea.productoNombre}",
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.width(8.dp))
        Text(
            text = Formatos.moneda(linea.importe),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        IconButton(onClick = onQuitar) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Eliminar ${linea.productoNombre}",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClienteSelector(
    clientes: List<Pair<String, String>>,
    clienteId: String?,
    onSeleccionar: (String?) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
    val nombre = clientes.firstOrNull { it.first == clienteId }?.second ?: "Sin cliente"

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
    ) {
        OutlinedTextField(
            value = nombre,
            onValueChange = {},
            readOnly = true,
            label = { Text("Cliente (opcional)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
        ) {
            DropdownMenuItem(
                text = { Text("Sin cliente") },
                onClick = {
                    onSeleccionar(null)
                    expandido = false
                },
            )
            clientes.forEach { (id, nombreCliente) ->
                DropdownMenuItem(
                    text = { Text(nombreCliente) },
                    onClick = {
                        onSeleccionar(id)
                        expandido = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TotalesBlock(subtotal: Double, impuesto: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(Formatos.moneda(subtotal), style = MaterialTheme.typography.bodyMedium)
        }
        Row {
            Text("IVA (13 %)", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text(Formatos.moneda(impuesto), style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = Formatos.moneda(subtotal + impuesto),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
