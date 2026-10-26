package com.shoppi.ui.ventas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoppi.data.model.Producto
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.components.SearchBar
import com.shoppi.ui.components.StockBadge
import com.shoppi.ui.theme.Dimens
import com.shoppi.util.Formatos

/**
 * Selector de productos del punto de venta (plan_ui_compose.md P6).
 *
 * Tocar un producto lo agrega al carrito; el total y la cantidad de líneas se
 * muestran en la barra inferior, que abre el carrito para confirmar la venta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuntoVentaScreen(
    onIrACarrito: () -> Unit,
    viewModel: VentaViewModel,
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val productos = remember(estado.productos, estado.busqueda) { viewModel.productosVisibles() }

    LaunchedEffect(estado.error) {
        estado.error?.let {
            snackbarHostState.showSnackbar(it)
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
        topBar = { TopAppBar(title = { Text("Punto de venta") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBar(
                valor = estado.busqueda,
                onValorChange = viewModel::buscar,
                placeholder = "Buscar producto",
                modifier = Modifier.padding(
                    horizontal = Dimens.ScreenPadding,
                    vertical = Dimens.InputPadding,
                ),
            )

            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = Dimens.ScreenPadding,
                    vertical = Dimens.InputPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (productos.isEmpty()) {
                    item {
                        EmptyState(
                            titulo = if (estado.busqueda.isBlank()) {
                                "No hay productos para vender"
                            } else {
                                "Sin resultados"
                            },
                            mensaje = if (estado.busqueda.isBlank()) {
                                "Agrega productos desde la pestaña Productos."
                            } else {
                                "Prueba con otro nombre o categoría."
                            },
                        )
                    }
                } else {
                    items(productos, key = { it.id }) { producto ->
                        ProductoVentaRow(
                            producto = producto,
                            enCarrito = estado.lineas
                                .firstOrNull { it.productoId == producto.id }
                                ?.cantidad ?: 0,
                            onAgregar = { viewModel.agregar(producto) },
                        )
                    }
                }
            }

            CarritoBar(
                cantidad = estado.totalLineas,
                total = estado.totales.total,
                habilitado = estado.lineas.isNotEmpty(),
                onIrACarrito = onIrACarrito,
            )
        }
    }
}

@Composable
private fun ProductoVentaRow(
    producto: Producto,
    enCarrito: Int,
    onAgregar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = producto.stockActual > 0, onClick = onAgregar)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatos.moneda(producto.precio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                StockBadge(
                    stockActual = producto.stockActual,
                    stockMinimo = producto.stockMinimo,
                )
                if (enCarrito > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "En carrito: $enCarrito",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Agregar ${producto.nombre}",
            tint = if (producto.stockActual > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** Barra inferior con la cantidad de ítems, el total y el acceso al carrito. */
@Composable
private fun CarritoBar(
    cantidad: Int,
    total: Double,
    habilitado: Boolean,
    onIrACarrito: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = if (cantidad == 1) "1 producto" else "$cantidad productos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = Formatos.moneda(total),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onIrACarrito,
                    enabled = habilitado,
                    modifier = Modifier.height(Dimens.ButtonHeight),
                ) {
                    Text("Ver carrito")
                }
            }
        }
    }
}
