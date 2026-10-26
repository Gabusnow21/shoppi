package com.shoppi.ui.inventario

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.MovimientoInventario
import com.shoppi.data.model.Producto
import com.shoppi.data.model.TipoMovimiento
import com.shoppi.data.repository.InventarioRepository
import com.shoppi.data.repository.ProductoRepository
import com.shoppi.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Rangos de fecha del historial de inventario. */
enum class RangoFiltro(val etiqueta: String) {
    TODO("Todo"),
    HOY("Hoy"),
    SIETE_DIAS("7 días"),
    TREINTA_DIAS("30 días"),
}

/**
 * Devuelve el rango `[inicio, fin]` en epoch millis para el filtro.
 *
 * `null` en cualquiera de los extremos significa "sin filtro" por ese lado.
 * Es una función pura (recibe el instante) para poder probarla en JVM.
 */
fun rangoAFechas(rango: RangoFiltro, instante: Long): Pair<Long?, Long?> {
    if (rango == RangoFiltro.TODO) return null to null

    val zona = ZoneId.systemDefault()
    val hoy = LocalDate.ofInstant(Instant.ofEpochMilli(instante), zona)
    val diasAtras = when (rango) {
        RangoFiltro.HOY -> 0L
        RangoFiltro.SIETE_DIAS -> 6L
        RangoFiltro.TREINTA_DIAS -> 29L
        RangoFiltro.TODO -> 0L
    }

    val inicio = hoy.minusDays(diasAtras)
        .atStartOfDay(zona).toInstant().toEpochMilli()
    val fin = hoy.atTime(LocalTime.MAX)
        .atZone(zona).toInstant().toEpochMilli()

    return inicio to fin
}

/** Estado de la pantalla de inventario. */
data class InventarioUiState(
    val productos: List<Producto> = emptyList(),
    val movimientos: List<MovimientoInventario> = emptyList(),
    val filtroProductoId: String? = null,
    val rango: RangoFiltro = RangoFiltro.TODO,
    val formularioVisible: Boolean = false,
    val productoId: String? = null,
    val tipoMovimiento: TipoMovimiento = TipoMovimiento.ENTRADA,
    val cantidad: String = "",
    val descripcion: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val mensajeExito: String? = null,
) {
    val stockActual: Int
        get() = productos.firstOrNull { it.id == productoId }?.stockActual ?: 0

    /** Entradas y salidas del conjunto filtrado, para el resumen superior. */
    val resumen: Pair<Int, Int>
        get() {
            var entradas = 0
            var salidas = 0
            movimientos.forEach { movimiento ->
                if (movimiento.tipoMovimiento == TipoMovimiento.ENTRADA) {
                    entradas += movimiento.cantidad
                } else {
                    salidas += movimiento.cantidad
                }
            }
            return entradas to salidas
        }
}

/**
 * ViewModel de inventario: historial filtrado y alta de movimientos.
 *
 * Los filtros viven en un [MutableStateFlow] propio porque el historial es un
 * Flow de Room parametrizado: cada cambio de filtro re-suscribe la consulta.
 */
class InventarioViewModel(
    private val inventario: InventarioRepository,
    private val productos: ProductoRepository,
    private val ahora: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private data class Filtro(val productoId: String? = null, val rango: RangoFiltro = RangoFiltro.TODO)

    private val filtro = MutableStateFlow(Filtro())

    private val _estado = MutableStateFlow(InventarioUiState())
    val estado: StateFlow<InventarioUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            productos.observar().collect { lista ->
                _estado.update { it.copy(productos = lista) }
            }
        }
        viewModelScope.launch {
            filtro.collect { f ->
                val (inicio, fin) = rangoAFechas(f.rango, ahora())
                inventario.observar(f.productoId, inicio, fin).collect { movimientos ->
                    _estado.update { it.copy(movimientos = movimientos) }
                }
            }
        }
    }

    // ----------------------------------------------------------------- filtros

    fun filtrarProducto(productoId: String?) {
        filtro.update { it.copy(productoId = productoId) }
        _estado.update { it.copy(filtroProductoId = productoId) }
    }

    fun filtrarRango(rango: RangoFiltro) {
        filtro.update { it.copy(rango = rango) }
        _estado.update { it.copy(rango = rango) }
    }

    // -------------------------------------------------------------- formulario

    fun abrirFormulario(productoId: String? = null) {
        _estado.update {
            it.copy(
                formularioVisible = true,
                productoId = productoId ?: it.filtroProductoId,
                cantidad = "",
                descripcion = "",
                error = null,
            )
        }
    }

    fun cerrarFormulario() {
        _estado.update { it.copy(formularioVisible = false, error = null) }
    }

    fun seleccionarProducto(productoId: String?) {
        _estado.update { it.copy(productoId = productoId) }
    }

    fun cambiarTipoMovimiento(tipo: TipoMovimiento) {
        _estado.update { it.copy(tipoMovimiento = tipo) }
    }

    fun cambiarCantidad(valor: String) {
        _estado.update { it.copy(cantidad = valor.filter(Char::isDigit).take(7)) }
    }

    fun cambiarDescripcion(valor: String) {
        _estado.update { it.copy(descripcion = valor.take(200)) }
    }

    fun registrar() {
        val actual = _estado.value
        if (actual.cargando) return

        val productoId = actual.productoId
        if (productoId.isNullOrBlank()) {
            _estado.update { it.copy(error = "Elegí un producto.") }
            return
        }
        val cantidad = actual.cantidad.toIntOrNull()
        if (cantidad == null || cantidad <= 0) {
            _estado.update { it.copy(error = "La cantidad debe ser mayor que 0.") }
            return
        }

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = inventario.registrar(
                productoId = productoId,
                tipoMovimiento = actual.tipoMovimiento,
                cantidad = cantidad,
                descripcion = actual.descripcion,
            )) {
                is Resultado.Ok -> _estado.update {
                    it.copy(
                        cargando = false,
                        formularioVisible = false,
                        productoId = null,
                        cantidad = "",
                        descripcion = "",
                        mensajeExito = "Movimiento registrado. El stock se " +
                            "actualiza al sincronizar.",
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    // ------------------------------------------------------------------ avisos

    fun limpiarError() {
        _estado.update { it.copy(error = null) }
    }

    fun limpiarMensajeExito() {
        _estado.update { it.copy(mensajeExito = null) }
    }

    companion object {
        fun factory(
            inventario: InventarioRepository,
            productos: ProductoRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                InventarioViewModel(inventario, productos) as T
        }
    }
}
