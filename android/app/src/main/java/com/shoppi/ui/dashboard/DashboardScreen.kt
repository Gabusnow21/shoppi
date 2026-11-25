package com.shoppi.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoppi.config.RemoteConfigManager
import com.shoppi.data.model.ProductoEnStock
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.components.ThemeSelector
import com.shoppi.ui.theme.Dimens
import com.shoppi.ui.theme.ThemeMode
import com.shoppi.util.Formatos

/**
 * Dashboard con el resumen del día (ventas, inventario y accesos rápidos).
 *
 * El resumen lo calcula PostgreSQL (`ReporteRepository`), así que sin conexión
 * se muestra un aviso con reintentar en vez de datos viejos.
 */
@Composable
fun DashboardScreen(
    nombreUsuario: String?,
    onCerrarSesion: () -> Unit,
    onNuevaVenta: () -> Unit,
    onVerReportes: () -> Unit,
    themeMode: ThemeMode,
    onCambiarTema: (ThemeMode) -> Unit,
    viewModel: DashboardViewModel,
) {
    val syncHabilitado by RemoteConfigManager.syncEnabledFlow.collectAsState()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val refrescando by viewModel.refrescando.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = refrescando,
        onRefresh = viewModel::recargar,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardPadding),
        ) {
        if (!syncHabilitado) {
            Text(
                text = "Sincronización pausada por configuración remota.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hola, ${nombreUsuario ?: "usuario"}",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Resumen de hoy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = viewModel::recargar) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Actualizar resumen",
                )
            }
        }

        when {
            estado.cargando -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.CardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }

            estado.error != null && estado.ventasHoy == null &&
                estado.productosStockBajo == null -> {
                EmptyState(
                    titulo = "Sin conexión",
                    mensaje = estado.error ?: "No se pudo cargar el resumen.",
                    icono = Icons.Filled.Refresh,
                )
                Button(onClick = viewModel::cargar, modifier = Modifier.fillMaxWidth()) {
                    Text("Reintentar")
                }
            }

            else -> {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.InputPadding)) {
                    ResumenCard(
                        titulo = "Ventas de hoy",
                        valor = estado.ventasHoy?.toString() ?: "—",
                        detalle = estado.montoHoy?.let { Formatos.moneda(it) }
                            ?: "Sin datos",
                        modifier = Modifier.weight(1f),
                    )
                    ResumenCard(
                        titulo = "Stock bajo",
                        valor = estado.productosStockBajo?.toString() ?: "—",
                        detalle = estado.totalProductos?.let { "$it productos" }
                            ?: "Sin datos",
                        modifier = Modifier.weight(1f),
                        destacado = (estado.productosStockBajo ?: 0) > 0,
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.CardPadding),
                    ) {
                        Text(
                            text = "Valor del inventario",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = estado.valorInventario?.let { Formatos.moneda(it) } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (estado.bajos.isNotEmpty()) {
                    Text(
                        text = "Por reponer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    estado.bajos.take(3).forEach { producto ->
                        FilaStockBajo(producto)
                    }
                }

                if (estado.error != null) {
                    Text(
                        text = estado.error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
                    ) {
                        Text(
                            text = "Apariencia",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        ThemeSelector(
                            modo = themeMode,
                            onSeleccionar = onCambiarTema,
                        )
                    }
                }

                Button(
                    onClick = onNuevaVenta,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Nueva venta")
                }
                OutlinedButton(
                    onClick = onVerReportes,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ver reportes")
                }
                OutlinedButton(
                    onClick = onCerrarSesion,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Cerrar sesión")
                }
            }
        }

        Spacer(Modifier.height(Dimens.InputPadding))
        }
    }
}

@Composable
private fun ResumenCard(
    titulo: String,
    valor: String,
    detalle: String,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (destacado) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilaStockBajo(producto: ProductoEnStock) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Stock ${producto.stockActual} · mínimo ${producto.stockMinimo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
