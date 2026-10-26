package com.shoppi.ui.clientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.Cliente
import com.shoppi.data.repository.ClienteRepository
import com.shoppi.data.repository.Resultado
import com.shoppi.util.ClienteValidators
import com.shoppi.util.ErrorCliente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Campos del formulario de cliente como texto. */
data class ClienteFormUiState(
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
)

/**
 * ViewModel de alta/edición de cliente.
 *
 * Valida con [ClienteValidators] antes de escribir en Room, así el error que
 * ve el usuario es el mismo que usa el repositorio.
 */
class ClienteFormViewModel(
    private val clientes: ClienteRepository,
    private val clienteId: String?,
) : ViewModel() {

    private val _estado = MutableStateFlow(ClienteFormUiState())
    val estado: StateFlow<ClienteFormUiState> = _estado.asStateFlow()

    private var clienteOriginal: Cliente? = null

    init {
        if (!clienteId.isNullOrBlank()) {
            viewModelScope.launch {
                val cliente = clientes.porId(clienteId)
                if (cliente == null) {
                    _estado.update { it.copy(error = "El cliente no existe.") }
                } else {
                    clienteOriginal = cliente
                    _estado.value = ClienteFormUiState(
                        nombre = cliente.nombre,
                        telefono = cliente.telefono,
                        email = cliente.email,
                        direccion = cliente.direccion,
                    )
                }
            }
        }
    }

    fun cambiarNombre(valor: String) = actualizar { it.copy(nombre = valor) }
    fun cambiarTelefono(valor: String) = actualizar { it.copy(telefono = valor) }
    fun cambiarEmail(valor: String) = actualizar { it.copy(email = valor) }
    fun cambiarDireccion(valor: String) = actualizar { it.copy(direccion = valor) }

    fun guardar() {
        val actual = _estado.value
        if (actual.cargando) return

        val errores = ClienteValidators.errores(actual.nombre, actual.telefono, actual.email)
        if (errores.isNotEmpty()) {
            _estado.update { it.copy(error = mensaje(errores)) }
            return
        }

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = if (clienteOriginal == null) {
                clientes.crear(
                    nombre = actual.nombre,
                    telefono = actual.telefono,
                    email = actual.email,
                    direccion = actual.direccion,
                )
            } else {
                clientes.actualizar(
                    clienteOriginal!!.copy(
                        nombre = actual.nombre.trim(),
                        telefono = actual.telefono.trim(),
                        email = actual.email.trim(),
                        direccion = actual.direccion.trim(),
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

    private fun actualizar(transformacion: (ClienteFormUiState) -> ClienteFormUiState) {
        _estado.update { transformacion(it).copy(error = null) }
    }

    private fun mensaje(errores: Set<ErrorCliente>): String =
        errores.joinToString(". ") { error ->
            when (error) {
                ErrorCliente.NOMBRE_CORTO -> "El nombre debe tener al menos 3 caracteres"
                ErrorCliente.EMAIL_INVALIDO -> "El correo no tiene un formato válido"
                ErrorCliente.TELEFONO_INVALIDO -> "El teléfono solo puede contener números, espacios y signos"
            }
        }

    companion object {
        fun factory(
            clientes: ClienteRepository,
            clienteId: String?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ClienteFormViewModel(clientes, clienteId) as T
        }
    }
}
