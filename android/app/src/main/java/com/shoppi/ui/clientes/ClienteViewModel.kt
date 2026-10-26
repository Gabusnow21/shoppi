package com.shoppi.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.Cliente
import com.shoppi.data.repository.ClienteRepository
import com.shoppi.data.repository.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la lista de clientes. */
data class ClienteListUiState(
    val clientes: List<Cliente> = emptyList(),
    val busqueda: String = "",
    val error: String? = null,
    val eliminando: String? = null,
)

/**
 * ViewModel de clientes.
 *
 * Observa Room (fuente de verdad local) con el mismo patrón que
 * `ProductoViewModel`: la UI se actualiza al instante y el `SyncManager`
 * replica los cambios en segundo plano.
 */
class ClienteViewModel(private val clientes: ClienteRepository) : ViewModel() {

    private val busqueda = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)
    private val eliminando = MutableStateFlow<String?>(null)

    val estado: StateFlow<ClienteListUiState> = combine(
        clientes.observar(),
        busqueda,
        error,
        eliminando,
    ) { lista, consulta, mensajeError, idEliminando ->
        ClienteListUiState(
            clientes = filtrar(lista, consulta),
            busqueda = consulta,
            error = mensajeError,
            eliminando = idEliminando,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ClienteListUiState(),
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
            when (val resultado = clientes.eliminar(id)) {
                is Resultado.Ok -> Unit
                is Resultado.Error -> error.value = resultado.mensaje
            }
            eliminando.value = null
        }
    }

    private fun filtrar(lista: List<Cliente>, consulta: String): List<Cliente> {
        val texto = consulta.trim()
        if (texto.isEmpty()) return lista

        return lista.filter { cliente ->
            cliente.nombre.contains(texto, ignoreCase = true) ||
                cliente.telefono.contains(texto, ignoreCase = true) ||
                cliente.email.contains(texto, ignoreCase = true) ||
                cliente.direccion.contains(texto, ignoreCase = true)
        }
    }

    companion object {
        fun factory(clientes: ClienteRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ClienteViewModel(clientes) as T
            }
    }
}
