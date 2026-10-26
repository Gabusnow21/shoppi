package com.shoppi.ui.inventario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoppi.ShoppiApp
import com.shoppi.data.model.MovimientoInventario
import com.shoppi.data.model.SyncState
import com.shoppi.data.model.TipoMovimiento
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.theme.Dimens
import com.shoppi.util.Formatos

/**
 * Historial de inventario y alta de movimientos (plan_ui_compose.md).
 *
 * Filtra por producto y rango de fechas, resume entradas y salidas y permite
 * registrar un movimiento ENTRADA/SALIDA con un diálogo. El movimiento se
 * guarda en Room con `syncState = PENDING` y lo sincroniza el `SyncManager`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    onVolver: () -> Unit,
    viewModel: InventarioViewModel = viewModel(factory = inventarioViewModelFactory()),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(estado.error) {
        val error = estado.error ?: return@LaunchedEffect
        // Con el diálogo abierto el error se muestra ahí dentro, no en el snackbar.
        if (!estado.formularioVisible) {
            snackbarHostState.showSnackbar(error)
            viewModel.limpiarError()
        }
    }

    LaunchedEffect(estado.mensajeExito) {
        estado.mensajeExito?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajeExito()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventario") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.abrirFormulario() },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Movimiento") },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FiltrosBlock(estado = estado, viewModel = viewModel)

            val (entradas, salidas) = estado.resumen
            Text(
                text = "Entradas: $entradas · Salidas: $salidas · " +
                    "${estado.movimientos.size} movimiento(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = Dimens.ScreenPadding,
                    vertical = Dimens.InputPadding,
                ),
            )

            if (estado.movimientos.isEmpty()) {
                EmptyState(
                    titulo = "Sin movimientos",
                    mensaje = "Registrá una entrada o salida con el botón +.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = Dimens.ScreenPadding,
                        vertical = Dimens.InputPadding,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(estado.movimientos, key = { it.id }) { movimiento ->
                        MovimientoRow(movimiento = movimiento)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }

    if (estado.formularioVisible) {
        FormularioMovimientoDialog(estado = estado, viewModel = viewModel)
    }
}

/** Filtros: rango de fechas (chips) y producto (dropdown). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltrosBlock(estado: InventarioUiState, viewModel: InventarioViewModel) {
    var productoExpandido by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RangoFiltro.entries.forEach { rango ->
                FilterChip(
                    selected = estado.rango == rango,
                    onClick = { viewModel.filtrarRango(rango) },
                    label = { Text(rango.etiqueta) },
                )
            }
        }

        ExposedDropdownMenuBox(
            expanded = productoExpandido,
            onExpandedChange = { productoExpandido = it },
        ) {
            OutlinedTextField(
                value = nombreProductoFiltrado(estado),
                onValueChange = {},
                readOnly = true,
                label = { Text("Producto") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = productoExpandido)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = productoExpandido,
                onDismissRequest = { productoExpandido = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Todos los productos") },
                    onClick = {
                        viewModel.filtrarProducto(null)
                        productoExpandido = false
                    },
                )
                estado.productos.forEach { producto ->
                    DropdownMenuItem(
                        text = { Text("${producto.nombre} (${producto.stockActual})") },
                        onClick = {
                            viewModel.filtrarProducto(producto.id)
                            productoExpandido = false
                        },
                    )
                }
            }
        }
    }
}

private fun nombreProductoFiltrado(estado: InventarioUiState): String {
    val id = estado.filtroProductoId ?: return "Todos los productos"
    return estado.productos.firstOrNull { it.id == id }?.nombre ?: "Todos los productos"
}

/** Fila del historial: producto, fecha, descripción y cantidad con signo. */
@Composable
private fun MovimientoRow(movimiento: MovimientoInventario) {
    val esEntrada = movimiento.tipoMovimiento == TipoMovimiento.ENTRADA
    val color = if (esEntrada) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (esEntrada) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (esEntrada) "Entrada" else "Salida",
                tint = color,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(Modifier.width(Dimens.InputPadding))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = movimiento.productoNombre.ifBlank { "Producto" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = buildString {
                    append(if (esEntrada) "Entrada" else "Salida")
                    if (movimiento.descripcion.isNotBlank()) {
                        append(" · ")
                        append(movimiento.descripcion)
                    }
                    append(" · ")
                    append(Formatos.fecha(movimiento.fecha))
                    if (movimiento.syncState != SyncState.SYNCED) {
                        append(" · pendiente de sincronizar")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = if (esEntrada) "+${movimiento.cantidad}" else "-${movimiento.cantidad}",
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Diálogo de alta de un movimiento de inventario. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioMovimientoDialog(
    estado: InventarioUiState,
    viewModel: InventarioViewModel,
) {
    var productoExpandido by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!estado.cargando) viewModel.cerrarFormulario() },
        title = { Text("Registrar movimiento") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
            ) {
                ExposedDropdownMenuBox(
                    expanded = productoExpandido,
                    onExpandedChange = { productoExpandido = !productoExpandido },
                ) {
                    OutlinedTextField(
                        value = estado.productos
                            .firstOrNull { it.id == estado.productoId }
                            ?.let { "${it.nombre} · stock ${it.stockActual}" }
                            .orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto") },
                        placeholder = { Text("Elegí un producto") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = productoExpandido)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = productoExpandido,
                        onDismissRequest = { productoExpandido = false },
                    ) {
                        estado.productos.forEach { producto ->
                            DropdownMenuItem(
                                text = { Text("${producto.nombre} (${producto.stockActual})") },
                                onClick = {
                                    viewModel.seleccionarProducto(producto.id)
                                    productoExpandido = false
                                },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoMovimiento.entries.forEach { tipo ->
                        FilterChip(
                            selected = estado.tipoMovimiento == tipo,
                            onClick = { viewModel.cambiarTipoMovimiento(tipo) },
                            label = {
                                Text(if (tipo == TipoMovimiento.ENTRADA) "Entrada" else "Salida")
                            },
                        )
                    }
                }

                OutlinedTextField(
                    value = estado.cantidad,
                    onValueChange = viewModel::cambiarCantidad,
                    label = { Text("Cantidad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = estado.descripcion,
                    onValueChange = viewModel::cambiarDescripcion,
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (estado.error != null) {
                    Text(
                        text = estado.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Text(
                    text = "El stock se actualiza en el servidor al sincronizar " +
                        "el movimiento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::registrar,
                enabled = !estado.cargando,
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Registrar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.cerrarFormulario() },
                enabled = !estado.cargando,
            ) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun inventarioViewModelFactory(): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container) {
        InventarioViewModel.factory(
            inventario = container.inventario,
            productos = container.productos,
        )
    }
}
