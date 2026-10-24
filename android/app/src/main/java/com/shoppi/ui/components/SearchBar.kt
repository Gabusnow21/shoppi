package com.shoppi.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Buscador reutilizable para listas (productos, clientes). */
@Composable
fun SearchBar(
    valor: String,
    onValorChange: (String) -> Unit,
    placeholder: String = "Buscar",
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (valor.isNotEmpty()) {
                IconButton(onClick = { onValorChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpiar búsqueda",
                    )
                }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}
