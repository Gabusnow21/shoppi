package com.shoppi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoppi.data.model.PeriodoReporte
import com.shoppi.data.model.ProductoEnStock
import com.shoppi.data.repository.ReporteRepository
import com.shoppi.data.repository.Resultado
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Resumen del día que muestra el dashboard. */
data class DashboardEstado(
    val cargando: Boolean = true,
    /** Ventas registradas hoy (null si aún no se pudo consultar). */
    val ventasHoy: Long? = null,
    /** Monto vendido hoy. */
    val montoHoy: Double? = null,
    /** Productos con stock por debajo del mínimo. */
    val productosStockBajo: Int? = null,
    /** Productos con stock bajo, para listar los primeros. */
    val bajos: List<ProductoEnStock> = emptyList(),
    /** Valor total del inventario. */
    val valorInventario: Double? = null,
    /** Productos distintos en inventario. */
    val totalProductos: Int? = null,
    val error: String? = null,
)

/**
 * Carga el resumen del dashboard.
 *
 * Los datos vienen de `ReporteRepository` (agregados que calcula PostgreSQL),
 * así que son consultas de solo lectura contra la API: sin conexión se muestra
 * el error y se permite reintentar o arrastrar para recargar.
 */
class DashboardViewModel(
    private val reportes: ReporteRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(DashboardEstado())
    val estado: StateFlow<DashboardEstado> = _estado.asStateFlow()

    /** Recarga en curso disparada por pull-to-refresh (sin tapar la pantalla). */
    private val _refrescando = MutableStateFlow(false)
    val refrescando: StateFlow<Boolean> = _refrescando.asStateFlow()

    init {
        cargar()
    }

    /** Carga inicial: muestra el indicador central. */
    fun cargar() {
        _estado.value = DashboardEstado(cargando = true)
        viewModelScope.launch {
            _estado.value = consultar().copy(cargando = false)
        }
    }

    /** Recarga por pull-to-refresh: conserva el contenido mientras consulta. */
    fun recargar() {
        if (_refrescando.value) return
        _refrescando.value = true
        viewModelScope.launch {
            _estado.value = consultar().copy(cargando = false)
            _refrescando.value = false
        }
    }

    private suspend fun consultar(): DashboardEstado = coroutineScope {
        val ventasDeHoy = async { reportes.ventas(PeriodoReporte.DIARIO) }
        val inventario = async { reportes.inventario() }

        val resumenVentas = ventasDeHoy.await()
        val resumenInventario = inventario.await()

        val error = (resumenVentas as? Resultado.Error)?.mensaje
            ?: (resumenInventario as? Resultado.Error)?.mensaje

        DashboardEstado(
            ventasHoy = (resumenVentas as? Resultado.Ok)?.datos?.totalVentas,
            montoHoy = (resumenVentas as? Resultado.Ok)?.datos?.montoTotal,
            productosStockBajo = (resumenInventario as? Resultado.Ok)
                ?.datos?.productosStockBajo,
            bajos = (resumenInventario as? Resultado.Ok)
                ?.datos?.productos
                ?.filter { it.stockBajo }
                ?.sortedBy { it.stockActual - it.stockMinimo }
                .orEmpty(),
            valorInventario = (resumenInventario as? Resultado.Ok)
                ?.datos?.valorInventario,
            totalProductos = (resumenInventario as? Resultado.Ok)
                ?.datos?.totalProductos,
            error = error,
        )
    }

    companion object {
        fun factory(reportes: ReporteRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { DashboardViewModel(reportes) }
            }
    }
}
