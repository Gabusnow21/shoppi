package com.shoppi.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoppi.ShoppiApp
import com.shoppi.ui.theme.Dimens

/**
 * Pantalla de inicio de sesión (plan_ui_compose.md P1).
 *
 * Layout corto sin scroll rígido: usa scroll + `imePadding()` para que el
 * teclado no tape el botón en pantallas pequeñas.
 */
@Composable
fun LoginScreen(
    onLoginExitoso: () -> Unit,
    onIrARegistro: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = loginViewModelFactory()),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(estado.usuario) {
        if (estado.usuario != null) {
            viewModel.consumirSesion()
            onLoginExitoso()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Shoppi",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Punto de venta e inventario",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = estado.email,
            onValueChange = viewModel::cambiarEmail,
            label = { Text("Correo") },
            singleLine = true,
            enabled = !estado.cargando,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.InputPadding))

        OutlinedTextField(
            value = estado.password,
            onValueChange = viewModel::cambiarPassword,
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !estado.cargando,
            visualTransformation = if (estado.passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = viewModel::alternarVisibilidadPassword) {
                    Icon(
                        imageVector = if (estado.passwordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (estado.passwordVisible) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    viewModel.iniciarSesion()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = estado.recordarSesion,
                onCheckedChange = viewModel::cambiarRecordarSesion,
                enabled = !estado.cargando,
            )
            Text(
                text = "Recordar sesión",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (estado.error != null) {
            Text(
                text = estado.error.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Dimens.InputPadding))
        }

        Button(
            onClick = {
                focusManager.clearFocus()
                viewModel.iniciarSesion()
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
                Text("Iniciar sesión")
            }
        }

        Spacer(Modifier.height(Dimens.InputPadding))

        TextButton(
            onClick = onIrARegistro,
            enabled = !estado.cargando,
        ) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}

@Composable
private fun loginViewModelFactory(): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container) { LoginViewModel.factory(container.usuarios) }
}
