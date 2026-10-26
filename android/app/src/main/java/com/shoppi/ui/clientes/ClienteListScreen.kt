package com.shoppi.ui.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
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
import com.shoppi.data.model.Cliente
import com.shoppi.ui.components.ClienteCard
import com.shoppi.ui.components.ConfirmDialog
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.components.SearchBar
import com.shoppi.ui.theme.Dimens

/**
 * Lista de clientes (plan_ui_compose.md, patrón de productos).
 *
 * Lista vertical con búsqueda, estado de sincronización y borrado lógico con
 * confirmación. Los datos salen de Room, no de la red.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteListScreen(
    onAgregar: () -> Unit,
    onEditar: (String) -> Unit,
    viewModel: ClienteViewModel = viewModel(factory = clienteViewModelFactory()),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var clienteAEliminar by remember { mutableStateOf<Cliente?>(null) }

    LaunchedEffect(estado.error) {
        estado.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Clientes") }) },
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
                placeholder = "Buscar por nombre, teléfono o correo",
                modifier = Modifier.padding(
                    horizontal = Dimens.ScreenPadding,
                    vertical = Dimens.InputPadding,
                ),
            )

            LazyColumn(
                contentPadding = PaddingValues(
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding,
                    bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (estado.clientes.isEmpty()) {
                    item {
                        EmptyState(
                            titulo = if (estado.busqueda.isBlank()) {
                                "Todavía no hay clientes"
                            } else {
                                "Sin resultados"
                            },
                            mensaje = if (estado.busqueda.isBlank()) {
                                "Agrega tu primer cliente con el botón +."
                            } else {
                                "Prueba con otro nombre, teléfono o correo."
                            },
                            icono = Icons.Filled.People,
                        )
                    }
                } else {
                    items(estado.clientes, key = { it.id }) { cliente ->
                        ClienteCard(
                            cliente = cliente,
                            onClick = { onEditar(cliente.id) },
                            onEliminar = { clienteAEliminar = cliente },
                        )
                    }
                }
            }
        }
    }

    clienteAEliminar?.let { cliente ->
        ConfirmDialog(
            titulo = "Eliminar cliente",
            mensaje = "¿Seguro que quieres eliminar \"${cliente.nombre}\"?",
            onConfirmar = {
                viewModel.eliminar(cliente.id)
                clienteAEliminar = null
            },
            onCancelar = { clienteAEliminar = null },
        )
    }
}

@Composable
private fun clienteViewModelFactory(): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container) { ClienteViewModel.factory(container.clientes) }
}
