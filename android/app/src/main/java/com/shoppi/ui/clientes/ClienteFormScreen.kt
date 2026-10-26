package com.shoppi.ui.clientes

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
 * Alta/edición de cliente.
 *
 * Todos los campos son opcionales menos el nombre; teléfono y correo se
 * validan con `ClienteValidators` si se escriben.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteFormScreen(
    clienteId: String?,
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    viewModel: ClienteFormViewModel = viewModel(
        factory = clienteFormViewModelFactory(clienteId),
    ),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val editando = !clienteId.isNullOrBlank()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) {
            viewModel.consumirGuardado()
            onGuardado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editando) "Editar cliente" else "Nuevo cliente") },
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
                label = { Text("Nombre completo") },
                singleLine = true,
                enabled = !estado.cargando,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.telefono,
                onValueChange = viewModel::cambiarTelefono,
                label = { Text("Teléfono (opcional)") },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.email,
                onValueChange = viewModel::cambiarEmail,
                label = { Text("Correo (opcional)") },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.direccion,
                onValueChange = viewModel::cambiarDireccion,
                label = { Text("Dirección (opcional)") },
                minLines = 2,
                maxLines = 3,
                enabled = !estado.cargando,
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
                    Text(if (editando) "Guardar cambios" else "Crear cliente")
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
private fun clienteFormViewModelFactory(
    clienteId: String?,
): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container, clienteId) {
        ClienteFormViewModel.factory(container.clientes, clienteId)
    }
}
