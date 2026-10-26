package com.shoppi.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.Cliente
import com.shoppi.data.model.MetodoPago
import com.shoppi.data.model.Producto
import com.shoppi.data.model.VentaItem
import com.shoppi.data.repository.ClienteRepository
import com.shoppi.data.repository.ProductoRepository
import com.shoppi.data.repository.Resultado
import com.shoppi.data.repository.VentaRepository
import com.shoppi.util.Formatos
import com.shoppi.util.VentaCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Línea del carrito con el stock disponible del producto. */
data class LineaCarrito(
    val productoId: String,
    val productoNombre: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val stockMaximo: Int,
) {
    val importe: Double
        get() = cantidad * precioUnitario

    fun aVentaItem(): VentaItem = VentaItem(
        productoId = productoId,
        productoNombre = productoNombre,
        cantidad = cantidad,
        precioUnitario = precioUnitario,
    )
}

/** Estado del punto de venta: catálogo, carrito y datos de cobro. */
data class VentaUiState(
    val productos: List<Producto> = emptyList(),
    val clientes: List<Cliente> = emptyList(),
    val busqueda: String = "",
    val lineas: List<LineaCarrito> = emptyList(),
    val clienteId: String? = null,
    val metodoPago: MetodoPago = MetodoPago.EFECTIVO,
    val cargando: Boolean = false,
    val error: String? = null,
    val mensajeExito: String? = null,
) {
    val totalLineas: Int
        get() = lineas.sumOf { it.cantidad }

    val totales: VentaCalculator.Totales
        get() = VentaCalculator.calcularTotales(lineas.map { it.aVentaItem() })
}

/**
 * ViewModel del punto de venta.
 *
 * El carrito vive en memoria y comparte entre la pantalla de productos y la
 * del carrito (se instancia una sola vez en el NavGraph). La venta se guarda
 * en Room con `syncState = PENDING` y el `SyncManager` la empuja después: el
 * backend es quien descuenta el stock de forma transaccional.
 */
class VentaViewModel(
    private val ventas: VentaRepository,
    private val productos: ProductoRepository,
    private val clientes: ClienteRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(VentaUiState())
    val estado: StateFlow<VentaUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            productos.observar().collect { lista ->
                _estado.update { it.copy(productos = lista) }
            }
        }
        viewModelScope.launch {
            clientes.observar().collect { lista ->
                _estado.update { it.copy(clientes = lista) }
            }
        }
    }

    fun buscar(valor: String) {
        _estado.update { it.copy(busqueda = valor) }
    }

    /** Productos filtrados por nombre o categoría, para el selector. */
    fun productosVisibles(): List<Producto> {
        val texto = _estado.value.busqueda.trim()
        if (texto.isEmpty()) return _estado.value.productos
        return _estado.value.productos.filter { producto ->
            producto.nombre.contains(texto, ignoreCase = true) ||
                producto.categoria.contains(texto, ignoreCase = true)
        }
    }

    fun agregar(producto: Producto) {
        val actual = _estado.value
        val existente = actual.lineas.firstOrNull { it.productoId == producto.id }

        if (producto.stockActual <= 0) {
            _estado.update { it.copy(error = "\"${producto.nombre}\" no tiene stock disponible.") }
            return
        }

        if (existente == null) {
            _estado.update {
                it.copy(
                    lineas = it.lineas + LineaCarrito(
                        productoId = producto.id,
                        productoNombre = producto.nombre,
                        precioUnitario = producto.precio,
                        cantidad = 1,
                        stockMaximo = producto.stockActual,
                    ),
                )
            }
        } else {
            incrementar(producto.id)
        }
    }

    fun incrementar(productoId: String) {
        val linea = _estado.value.lineas.firstOrNull { it.productoId == productoId } ?: return

        if (linea.cantidad >= linea.stockMaximo) {
            _estado.update {
                it.copy(error = "Solo hay ${linea.stockMaximo} unidades de \"${linea.productoNombre}\".")
            }
            return
        }

        actualizarLinea(productoId) { it.copy(cantidad = it.cantidad + 1) }
    }

    fun decrementar(productoId: String) {
        _estado.update { estado ->
            estado.copy(
                lineas = estado.lineas.mapNotNull { linea ->
                    when {
                        linea.productoId != productoId -> linea
                        linea.cantidad <= 1 -> null
                        else -> linea.copy(cantidad = linea.cantidad - 1)
                    }
                },
            )
        }
    }

    fun quitar(productoId: String) {
        _estado.update { it.copy(lineas = it.lineas.filterNot { l -> l.productoId == productoId }) }
    }

    fun limpiarCarrito() {
        _estado.update { it.copy(lineas = emptyList(), clienteId = null) }
    }

    fun seleccionarCliente(id: String?) {
        _estado.update { it.copy(clienteId = id) }
    }

    fun cambiarMetodoPago(metodo: MetodoPago) {
        _estado.update { it.copy(metodoPago = metodo) }
    }

    fun limpiarError() {
        _estado.update { it.copy(error = null) }
    }

    fun limpiarMensajeExito() {
        _estado.update { it.copy(mensajeExito = null) }
    }

    fun confirmar() {
        val actual = _estado.value
        if (actual.cargando) return
        if (actual.lineas.isEmpty()) {
            _estado.update { it.copy(error = "El carrito está vacío.") }
            return
        }

        val cliente = actual.clientes.firstOrNull { it.id == actual.clienteId }
        val items = actual.lineas.map { it.aVentaItem() }
        val total = VentaCalculator.calcularTotales(items).total

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = ventas.registrar(
                clienteId = cliente?.id,
                clienteNombre = cliente?.nombre.orEmpty(),
                items = items,
                metodoPago = actual.metodoPago,
            )

            when (resultado) {
                is Resultado.Ok -> _estado.update {
                    it.copy(
                        cargando = false,
                        lineas = emptyList(),
                        clienteId = null,
                        mensajeExito = "Venta de ${Formatos.moneda(total)} registrada.",
                    )
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    private fun actualizarLinea(
        productoId: String,
        transformacion: (LineaCarrito) -> LineaCarrito,
    ) {
        _estado.update { estado ->
            estado.copy(
                lineas = estado.lineas.map { linea ->
                    if (linea.productoId == productoId) transformacion(linea) else linea
                },
            )
        }
    }

    companion object {
        fun factory(
            ventas: VentaRepository,
            productos: ProductoRepository,
            clientes: ClienteRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                VentaViewModel(ventas, productos, clientes) as T
        }
    }
}
