package com.shoppi.ui.productos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoppi.ShoppiApp
import com.shoppi.ui.theme.Dimens

/**
 * Alta/edición de producto (plan_ui_compose.md P2b).
 *
 * La imagen se limita a `imagenUrl` por ahora; el picker + Firebase Storage
 * llega en la tarea 3B (T-241).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductoFormScreen(
    productoId: String?,
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: ProductoFormViewModel = viewModel(
        factory = productoFormViewModelFactory(productoId),
    ),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val editando = !productoId.isNullOrBlank()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) {
            viewModel.consumirGuardado()
            onGuardado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editando) "Editar producto" else "Nuevo producto") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
        ) {
            OutlinedTextField(
                value = estado.nombre,
                onValueChange = viewModel::cambiarNombre,
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = viewModel::cambiarDescripcion,
                label = { Text("Descripción (opcional)") },
                minLines = 2,
                maxLines = 4,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.precio,
                onValueChange = viewModel::cambiarPrecio,
                label = { Text("Precio") },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.stockActual,
                onValueChange = viewModel::cambiarStockActual,
                label = { Text("Stock actual") },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.stockMinimo,
                onValueChange = viewModel::cambiarStockMinimo,
                label = { Text("Stock mínimo") },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.categoria,
                onValueChange = viewModel::cambiarCategoria,
                label = { Text("Categoría (opcional)") },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            if (estado.error != null) {
                Text(
                    text = estado.error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.guardar()
                },
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
                    Text(if (editando) "Guardar cambios" else "Crear producto")
                }
            }

            Text(
                text = "Los cambios se guardan en el dispositivo y se sincronizan " +
                    "automáticamente cuando haya conexión.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun productoFormViewModelFactory(
    productoId: String?,
): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container, productoId) {
        ProductoFormViewModel.factory(container.productos, productoId)
    }
}
