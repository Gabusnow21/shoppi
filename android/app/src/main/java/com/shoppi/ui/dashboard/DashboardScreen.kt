package com.shoppi.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoppi.ui.theme.Dimens

/**
 * Dashboard temporal de la fase 2.2.
 *
 * Permite verificar el cierre de sesión (T-115) hasta que la tarea 2.7
 * implemente el dashboard real con reportes.
 */
@Composable
fun DashboardScreen(
    nombreUsuario: String?,
    onCerrarSesion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Hola, ${nombreUsuario ?: "usuario"}",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Dashboard (T-161)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.CardPadding))
        Button(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cerrar sesión")
        }
        Spacer(Modifier.height(Dimens.InputPadding))
        OutlinedButton(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ventas de hoy: próximamente")
        }
    }
}
