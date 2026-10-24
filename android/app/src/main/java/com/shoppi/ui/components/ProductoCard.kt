package com.shoppi.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shoppi.data.model.Producto
import com.shoppi.data.model.SyncState
import com.shoppi.ui.theme.Dimens
import com.shoppi.util.Formatos

/**
 * Tarjeta de producto para el grid de la lista.
 *
 * Muestra imagen remota (Coil) o un placeholder offline, precio, badge de stock
 * y el estado de sincronización cuando hay cambios pendientes.
 */
@Composable
fun ProductoCard(
    producto: Producto,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (producto.imagenUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                AsyncImage(
                    model = producto.imagenUrl,
                    contentDescription = producto.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                )
            }
        }

        Column(modifier = Modifier.padding(Dimens.CardPadding)) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = Formatos.moneda(producto.precio),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StockBadge(
                    stockActual = producto.stockActual,
                    stockMinimo = producto.stockMinimo,
                )
                Spacer(Modifier.width(4.dp))
                if (producto.syncState != SyncState.SYNCED) {
                    Text(
                        text = if (producto.syncState == SyncState.FAILED) "Error" else "Pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (producto.syncState == SyncState.FAILED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            IconButton(
                onClick = onEliminar,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Eliminar ${producto.nombre}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
