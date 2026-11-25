package com.shoppi.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoppi.data.model.Venta
import com.shoppi.data.repository.VentaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Lista de ventas del usuario activo, ordenadas por fecha descendente.
 *
 * Lee de Room (no de la red): muestra también las ventas que todavía no se
 * sincronizaron.
 */
class VentaListViewModel(
    ventas: VentaRepository,
) : ViewModel() {

    val ventas: StateFlow<List<Venta>> = ventas.observar()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    companion object {
        fun factory(ventas: VentaRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { VentaListViewModel(ventas) }
            }
    }
}
