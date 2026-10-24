package com.shoppi.ui.productos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.Producto
import com.shoppi.data.repository.ProductoRepository
import com.shoppi.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la lista de productos. */
data class ProductoListUiState(
    val productos: List<Producto> = emptyList(),
    val busqueda: String = "",
    val error: String? = null,
    val eliminando: String? = null,
)

/**
 * ViewModel de productos.
 *
 * Observa Room (fuente de verdad local): la UI se actualiza al instante y el
 * `SyncManager` se encarga de replicar los cambios en segundo plano.
 */
class ProductoViewModel(private val productos: ProductoRepository) : ViewModel() {

    private val busqueda = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)
    private val eliminando = MutableStateFlow<String?>(null)

    val estado: StateFlow<ProductoListUiState> = combine(
        productos.observar(),
        busqueda,
        error,
        eliminando,
    ) { lista, consulta, mensajeError, idEliminando ->
        ProductoListUiState(
            productos = filtrar(lista, consulta),
            busqueda = consulta,
            error = mensajeError,
            eliminando = idEliminando,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductoListUiState(),
    )

    fun buscar(valor: String) {
        busqueda.value = valor
    }

    fun limpiarError() {
        error.value = null
    }

    fun eliminar(id: String) {
        if (eliminando.value != null) return

        viewModelScope.launch {
            eliminando.value = id
            when (val resultado = productos.eliminar(id)) {
                is Resultado.Ok -> Unit
                is Resultado.Error -> error.value = resultado.mensaje
            }
            eliminando.value = null
        }
    }

    private fun filtrar(lista: List<Producto>, consulta: String): List<Producto> {
        val texto = consulta.trim()
        if (texto.isEmpty()) return lista

        return lista.filter { producto ->
            producto.nombre.contains(texto, ignoreCase = true) ||
                producto.categoria.contains(texto, ignoreCase = true)
        }
    }

    companion object {
        fun factory(productos: ProductoRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ProductoViewModel(productos) as T
            }
    }
}
