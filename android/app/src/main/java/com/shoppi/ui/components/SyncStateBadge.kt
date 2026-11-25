package com.shoppi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shoppi.data.model.SyncState

/**
 * Estado de sincronización unificado para listas, tarjetas y el ticket.
 *
 * Punto + texto con el mismo color y vocabulario en toda la app. Por defecto no
 * se muestra nada para [SyncState.SYNCED] (no ensucia las listas); el ticket
 * puede pedir mostrarlo con [mostrarSincronizado].
 */
@Composable
fun SyncStateBadge(
    state: SyncState,
    modifier: Modifier = Modifier,
    mostrarSincronizado: Boolean = false,
) {
    val (color, texto) = when (state) {
        SyncState.SYNCED -> if (mostrarSincronizado) {
            MaterialTheme.colorScheme.primary to "Sincronizado"
        } else {
            return
        }

        SyncState.PENDING -> MaterialTheme.colorScheme.tertiary to "Pendiente"
        SyncState.FAILED -> MaterialTheme.colorScheme.error to "Error"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Punto(color)
        Spacer(Modifier.width(4.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun Punto(color: Color) {
    Spacer(
        modifier = Modifier
            .size(8.dp)
            .background(color = color, shape = CircleShape),
    )
}
