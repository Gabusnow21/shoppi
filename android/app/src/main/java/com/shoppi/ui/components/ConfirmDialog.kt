package com.shoppi.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Confirmación para acciones destructivas (borrar producto, cliente, etc.). */
@Composable
fun ConfirmDialog(
    titulo: String,
    mensaje: String,
    textoConfirmar: String = "Eliminar",
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(textoConfirmar)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        },
    )
}
