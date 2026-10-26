package com.shoppi.ui.reportes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.PeriodoReporte
import com.shoppi.data.model.ReporteInventario
import com.shoppi.data.model.ReporteVentas
import com.shoppi.data.model.TopProducto
import com.shoppi.data.repository.ReporteRepository
import com.shoppi.data.repository.Resultado
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Pestañas del reportero. */
enum class PestanaReporte(val etiqueta: String) {
    VENTAS("Ventas"),
    TOP("Más vendidos"),
    INVENTARIO("Inventario"),
}

/** Estado del reportero. */
data class ReportesUiState(
    val pestana: PestanaReporte = PestanaReporte.VENTAS,
    val periodo: PeriodoReporte = PeriodoReporte.DIARIO,
    val ventas: ReporteVentas? = null,
    val top: List<TopProducto> = emptyList(),
    val inventario: ReporteInventario? = null,
    val cargando: Boolean = false,
    val error: String? = null,
)

/**
 * ViewModel del reportero.
 *
 * Los tres reportes son consultas de solo lectura contra la API (no hay datos
 * locales): cada pestaña carga al activarse y puede recargar a pedido. Solo hay
 * una carga en vuelo a la vez: cambiar de pestaña cancela la anterior.
 */
class ReportesViewModel(
    private val reportes: ReporteRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(ReportesUiState())
    val estado: StateFlow<ReportesUiState> = _estado.asStateFlow()

    private var carga: Job? = null

    init {
        cargar()
    }

    fun cambiarPestana(pestana: PestanaReporte) {
        if (_estado.value.pestana == pestana) return
        _estado.update { it.copy(pestana = pestana) }
        cargar()
    }

    fun cambiarPeriodo(periodo: PeriodoReporte) {
        _estado.update { it.copy(periodo = periodo) }
        cargar(pestana = PestanaReporte.VENTAS, periodo = periodo)
    }

    fun recargar() = cargar()

    fun limpiarError() {
        _estado.update { it.copy(error = null) }
    }

    private fun cargar(
        pestana: PestanaReporte = _estado.value.pestana,
        periodo: PeriodoReporte = _estado.value.periodo,
    ) {
        carga?.cancel()
        carga = viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }

            when (pestana) {
                PestanaReporte.VENTAS ->
                    guardar(reportes.ventas(periodo)) { datos ->
                        _estado.update { it.copy(ventas = datos) }
                    }

                PestanaReporte.TOP ->
                    guardar(reportes.productosMasVendidos()) { datos ->
                        _estado.update { it.copy(top = datos) }
                    }

                PestanaReporte.INVENTARIO ->
                    guardar(reportes.inventario()) { datos ->
                        _estado.update { it.copy(inventario = datos) }
                    }
            }

            _estado.update { it.copy(cargando = false) }
        }
    }

    private fun <T> guardar(resultado: Resultado<T>, alOk: (T) -> Unit) {
        when (resultado) {
            is Resultado.Ok -> alOk(resultado.datos)
            is Resultado.Error -> _estado.update { it.copy(error = resultado.mensaje) }
        }
    }

    companion object {
        fun factory(reportes: ReporteRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReportesViewModel(reportes) as T
            }
    }
}
