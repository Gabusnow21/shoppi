package com.shoppi.ui.reportes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shoppi.ShoppiApp
import com.shoppi.data.model.PeriodoReporte
import com.shoppi.data.model.ProductoEnStock
import com.shoppi.data.model.ReporteInventario
import com.shoppi.data.model.ReporteVentas
import com.shoppi.data.model.TopProducto
import com.shoppi.data.model.VentaPorDia
import com.shoppi.ui.components.EmptyState
import com.shoppi.ui.theme.Dimens
import com.shoppi.util.Formatos
import java.time.format.DateTimeFormatter

/**
 * Reportero (plan_ui_compose.md P8): ventas por período, top de productos más
 * vendidos y estado del inventario.
 *
 * Todo se consulta a la API en el momento: no hay caché local, así que la
 * pantalla avisa cuando no hay conexión y permite recargar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    viewModel: ReportesViewModel = viewModel(factory = reportesViewModelFactory()),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(estado.error) {
        estado.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes") },
                actions = {
                    IconButton(onClick = viewModel::recargar, enabled = !estado.cargando) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Recargar",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = estado.pestana.ordinal) {
                PestanaReporte.entries.forEach { pestana ->
                    Tab(
                        selected = estado.pestana == pestana,
                        onClick = { viewModel.cambiarPestana(pestana) },
                        text = { Text(pestana.etiqueta) },
                    )
                }
            }

            if (estado.cargando) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            when (estado.pestana) {
                PestanaReporte.VENTAS -> ContenidoVentas(
                    estado = estado,
                    onPeriodo = viewModel::cambiarPeriodo,
                )

                PestanaReporte.TOP -> ContenidoTop(top = estado.top)

                PestanaReporte.INVENTARIO -> ContenidoInventario(inventario = estado.inventario)
            }
        }
    }
}

// ------------------------------------------------------------------ ventas

@Composable
private fun ContenidoVentas(estado: ReportesUiState, onPeriodo: (PeriodoReporte) -> Unit) {
    val reporte = estado.ventas

    LazyColumn(
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoReporte.entries.forEach { periodo ->
                    PeriodoChip(
                        periodo = periodo,
                        seleccionado = estado.periodo == periodo,
                        onSeleccionar = { onPeriodo(periodo) },
                    )
                }
            }
        }

        if (reporte == null) {
            item { EmptyState(titulo = "Sin datos", mensaje = "Tocá recargar para ver el reporte.") }
            return@LazyColumn
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.InputPadding)) {
                StatCard(
                    titulo = "Ventas",
                    valor = reporte.totalVentas.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    titulo = "Monto total",
                    valor = Formatos.moneda(reporte.montoTotal),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = rangoTexto(reporte),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Text(
                text = "Ventas por día",
                style = MaterialTheme.typography.titleSmall,
            )
        }

        if (reporte.ventasPorDia.isEmpty()) {
            item {
                EmptyState(
                    titulo = "Sin ventas en el período",
                    mensaje = "Registrá ventas desde el punto de venta.",
                )
            }
        } else {
            val maximo = reporte.ventasPorDia.maxOf { it.monto }.coerceAtLeast(1.0)
            items(reporte.ventasPorDia, key = { it.fecha?.toString() ?: it.hashCode().toString() }) { dia ->
                FilaVentaDia(dia = dia, maximo = maximo)
            }
        }
    }
}

private fun rangoTexto(reporte: ReporteVentas): String {
    val inicio = reporte.fechaInicio?.format(FECHA) ?: "…"
    val fin = reporte.fechaFin?.format(FECHA) ?: "…"
    return "${reporte.periodo.etiqueta} · $inicio – $fin"
}

@Composable
private fun PeriodoChip(
    periodo: PeriodoReporte,
    seleccionado: Boolean,
    onSeleccionar: () -> Unit,
) {
    FilterChip(
        selected = seleccionado,
        onClick = onSeleccionar,
        label = { Text(periodo.etiqueta) },
    )}

@Composable
private fun FilaVentaDia(dia: VentaPorDia, maximo: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dia.fecha?.format(FECHA) ?: "Sin fecha",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(84.dp),
            )
            Text(
                text = if (dia.cantidad == 1L) "1 venta" else "${dia.cantidad} ventas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formatos.moneda(dia.monto),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        LinearProgressIndicator(
            progress = { (dia.monto / maximo).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
    }
}

// ---------------------------------------------------------- productos vendidos

@Composable
private fun ContenidoTop(top: List<TopProducto>) {
    if (top.isEmpty()) {
        EmptyState(
            titulo = "Sin ventas registradas",
            mensaje = "El ranking aparece cuando haya ventas sincronizadas.",
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
    ) {
        itemsIndexed(top, key = { _, producto -> producto.productoId }) { indice, producto ->
            val posicion = indice + 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "#$posicion",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(40.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    Text(
                        text = "${producto.cantidadVendida} unidad(es)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = Formatos.moneda(producto.montoTotal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// --------------------------------------------------------------- inventario

@Composable
private fun ContenidoInventario(inventario: ReporteInventario?) {
    val reporte = inventario
    if (reporte == null) {
        EmptyState(titulo = "Sin datos", mensaje = "Tocá recargar para ver el reporte.")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.InputPadding),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.InputPadding)) {
                StatCard(
                    titulo = "Productos",
                    valor = reporte.totalProductos.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    titulo = "Stock bajo",
                    valor = reporte.productosStockBajo.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    titulo = "Valor",
                    valor = Formatos.moneda(reporte.valorInventario),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Text(
                text = "Estado del stock",
                style = MaterialTheme.typography.titleSmall,
            )
        }

        if (reporte.productos.isEmpty()) {
            item {
                EmptyState(
                    titulo = "Sin productos",
                    mensaje = "Agregá productos para ver su estado de inventario.",
                )
            }
        } else {
            items(reporte.productos, key = { it.productoId }) { producto ->
                FilaProductoStock(producto = producto)
            }
        }
    }
}

@Composable
private fun FilaProductoStock(producto: ProductoEnStock) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = producto.nombre,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Text(
                text = "Stock ${producto.stockActual} / mín. ${producto.stockMinimo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (producto.stockBajo) {
            Text(
                text = "Stock bajo",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        Text(
            text = Formatos.moneda(producto.valorStock),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ------------------------------------------------------------------ auxiliares

@Composable
private fun StatCard(titulo: String, valor: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.CornerMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.InputPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun reportesViewModelFactory(): androidx.lifecycle.ViewModelProvider.Factory {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as ShoppiApp).container
    }
    return remember(container) { ReportesViewModel.factory(container.reportes) }
}

private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
