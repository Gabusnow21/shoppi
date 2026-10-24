package com.shoppi.ui.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoppi.ShoppiApp
import com.shoppi.data.model.Producto
import com.shoppi.ui.components.ConfirmDialog
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.components.ProductoCard
import com.shoppi.ui.components.SearchBar
import com.shoppi.ui.theme.Dimens

/**
 * Lista de productos (plan_ui_compose.md P5).
 *
 * Grid de 2 columnas con búsqueda, badge de stock, estado de sincronización y
 * borrado lógico con confirmación. Los datos salen de Room, no de la red.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoListScreen(
    onAgregar: () -> Unit,
    onEditar: (String) -> Unit,
    viewModel: ProductoViewModel = viewModel(factory = productoViewModelFactory()),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var productoAEliminar by remember { mutableStateOf<Producto?>(null) }

    LaunchedEffect(estado.error) {
        estado.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Productos") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAgregar,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agregar") },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchBar(
                valor = estado.busqueda,
                onValorChange = viewModel::buscar,
                placeholder = "Buscar por nombre o categoría",
                modifier = Modifier.padding(
                    horizontal = Dimens.ScreenPadding,
                    vertical = Dimens.InputPadding,
                ),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    bottom = 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (estado.productos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            titulo = if (estado.busqueda.isBlank()) {
                                "Todavía no hay productos"
                            } else {
                                "Sin resultados"
                            },
                            mensaje = if (estado.busqueda.isBlank()) {
                                "Agrega tu primer producto con el botón +."
                            } else {
                                "Prueba con otro nombre o categoría."
                            },
                        )
                    }
                } else {
                    items(estado.productos, key = { it.id }) { producto ->
                        ProductoCard(
                            producto = producto,
                            onClick = { onEditar(producto.id) },
                            onEliminar = { productoAEliminar = producto },
                        )
                    }
                }
            }
        }
    }

    productoAEliminar?.let { producto ->
        ConfirmDialog(
            titulo = "Eliminar producto",
            mensaje = "¿Seguro que quieres eliminar \"${producto.nombre}\"?",
            onConfirmar = {
                viewModel.eliminar(producto.id)
                productoAEliminar = null
            },
            onCancelar = { productoAEliminar = null },
        )
    }
}

@Composable
private fun productoViewModelFactory(): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container) { ProductoViewModel.factory(container.productos) }
}
