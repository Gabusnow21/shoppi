package com.shoppi.ui.ventas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shoppi.data.model.Venta
import com.shoppi.data.repository.VentaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Estado de la pantalla del ticket. */
data class TicketEstado(
    val cargando: Boolean = true,
    val venta: Venta? = null,
    val error: String? = null,
)

/**
 * Carga la venta que muestra el ticket.
 *
 * El id que llega del push es el **id remoto** (el de PostgreSQL), así que se
 * busca por `remoteId` en Room. Si no está local todavía (la venta sigue en
 * cola de sincronización o se limpió la caché), se avisa y se permite reintentar.
 */
class TicketViewModel(
    private val ventaId: Long,
    private val ventas: VentaRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(TicketEstado())
    val estado: StateFlow<TicketEstado> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.value = TicketEstado(cargando = true)
        viewModelScope.launch {
            val venta = withContext(Dispatchers.IO) { ventas.porRemoteId(ventaId) }
            _estado.value = if (venta != null) {
                TicketEstado(cargando = false, venta = venta)
            } else {
                TicketEstado(
                    cargando = false,
                    error = "No encontramos la venta #$ventaId en este dispositivo. "
                        + "Si está pendiente de sincronizar, volvé a intentar en unos segundos.",
                )
            }
        }
    }

    companion object {
        fun factory(
            ventaId: Long,
            ventas: VentaRepository,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TicketViewModel(ventaId, ventas) }
        }
    }
}
