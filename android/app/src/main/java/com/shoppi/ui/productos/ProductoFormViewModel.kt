package com.shoppi.ui.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.Producto
import com.shoppi.data.repository.ProductoRepository
import com.shoppi.data.repository.Resultado
import com.shoppi.util.ErrorProducto
import com.shoppi.util.ProductoValidators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campos del formulario de producto como texto. */
data class ProductoFormUiState(
    val nombre: String = "",
    val descripcion: String = "",
    val precio: String = "",
    val stockActual: String = "0",
    val stockMinimo: String = "5",
    val categoria: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
)

/**
 * ViewModel de alta/edición de producto.
 *
 * Usa [ProductoValidators.FormularioProducto] para validar exactamente igual
 * que el repositorio antes de escribir en Room.
 */
class ProductoFormViewModel(
    private val productos: ProductoRepository,
    private val productoId: String?,
) : ViewModel() {

    private val _estado = MutableStateFlow(ProductoFormUiState())
    val estado: StateFlow<ProductoFormUiState> = _estado.asStateFlow()

    private var productoOriginal: Producto? = null

    init {
        if (!productoId.isNullOrBlank()) {
            viewModelScope.launch {
                val producto = productos.porId(productoId)
                if (producto == null) {
                    _estado.update { it.copy(error = "El producto no existe.") }
                } else {
                    productoOriginal = producto
                    _estado.value = ProductoFormUiState(
                        nombre = producto.nombre,
                        descripcion = producto.descripcion,
                        precio = producto.precio.toString(),
                        stockActual = producto.stockActual.toString(),
                        stockMinimo = producto.stockMinimo.toString(),
                        categoria = producto.categoria,
                    )
                }
            }
        }
    }

    fun cambiarNombre(valor: String) = actualizar { it.copy(nombre = valor) }
    fun cambiarDescripcion(valor: String) = actualizar { it.copy(descripcion = valor) }
    fun cambiarPrecio(valor: String) = actualizar { it.copy(precio = valor) }
    fun cambiarStockActual(valor: String) = actualizar { it.copy(stockActual = valor) }
    fun cambiarStockMinimo(valor: String) = actualizar { it.copy(stockMinimo = valor) }
    fun cambiarCategoria(valor: String) = actualizar { it.copy(categoria = valor) }

    fun guardar() {
        val actual = _estado.value
        if (actual.cargando) return

        val formulario = ProductoValidators.FormularioProducto(
            nombre = actual.nombre,
            precio = actual.precio,
            stockActual = actual.stockActual,
            stockMinimo = actual.stockMinimo,
        )
        val errores = ProductoValidators.errores(formulario)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(error = mensaje(errores)) }
            return
        }

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = if (productoOriginal == null) {
                productos.crear(
                    nombre = actual.nombre,
                    descripcion = actual.descripcion,
                    precio = formulario.precio.aDouble(),
                    stockActual = formulario.stockActual.trim().toInt(),
                    stockMinimo = formulario.stockMinimo.trim().toInt(),
                    categoria = actual.categoria,
                )
            } else {
                productos.actualizar(
                    productoOriginal!!.copy(
                        nombre = actual.nombre.trim(),
                        descripcion = actual.descripcion.trim(),
                        precio = formulario.precio.aDouble(),
                        stockActual = formulario.stockActual.trim().toInt(),
                        stockMinimo = formulario.stockMinimo.trim().toInt(),
                        categoria = actual.categoria.trim(),
                    ),
                )
            }

            when (resultado) {
                is Resultado.Ok -> _estado.update {
                    it.copy(cargando = false, guardado = true)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun consumirGuardado() {
        _estado.update { it.copy(guardado = false) }
    }

    private fun actualizar(transformacion: (ProductoFormUiState) -> ProductoFormUiState) {
        _estado.update { transformacion(it).copy(error = null) }
    }

    private fun mensaje(errores: Set<ErrorProducto>): String =
        errores.joinToString(". ") { error ->
            when (error) {
                ErrorProducto.NOMBRE_CORTO -> "El nombre debe tener al menos 2 caracteres"
                ErrorProducto.PRECIO_INVALIDO -> "El precio debe ser un número mayor o igual a 0"
                ErrorProducto.STOCK_INVALIDO -> "El stock debe ser un entero mayor o igual a 0"
                ErrorProducto.STOCK_MINIMO_INVALIDO ->
                    "El stock mínimo debe ser un entero mayor o igual a 0"
            }
        }

    private fun String.aDouble(): Double = trim().replace(',', '.').toDouble()

    companion object {
        fun factory(
            productos: ProductoRepository,
            productoId: String?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProductoFormViewModel(productos, productoId) as T
        }
    }
}
