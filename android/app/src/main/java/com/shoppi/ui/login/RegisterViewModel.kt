package com.shoppi.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.Usuario
import com.shoppi.data.repository.Resultado
import com.shoppi.data.repository.UsuarioRepository
import com.shoppi.util.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del formulario de registro. */
data class RegisterUiState(
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val passwordVisible: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val usuario: Usuario? = null,
)

/**
 * ViewModel del registro. La validación de formato vive en [Validators] y en
 * [UsuarioRepository]; aquí solo se comprueba que las contraseñas coincidan.
 */
class RegisterViewModel(private val usuarios: UsuarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RegisterUiState())
    val estado: StateFlow<RegisterUiState> = _estado.asStateFlow()

    fun cambiarNombre(valor: String) = actualizar { it.copy(nombre = valor) }
    fun cambiarEmail(valor: String) = actualizar { it.copy(email = valor) }
    fun cambiarTelefono(valor: String) = actualizar { it.copy(telefono = valor) }
    fun cambiarPassword(valor: String) = actualizar { it.copy(password = valor) }
    fun cambiarConfirmarPassword(valor: String) = actualizar { it.copy(confirmarPassword = valor) }

    fun alternarVisibilidadPassword() {
        _estado.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun registrarse() {
        val actual = _estado.value
        if (actual.cargando) return

        if (!Validators.contrasenasCoinciden(actual.password, actual.confirmarPassword)) {
            _estado.update { it.copy(error = "Las contraseñas no coinciden.") }
            return
        }

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = usuarios.registrar(
                nombre = actual.nombre,
                email = actual.email,
                password = actual.password,
                telefono = actual.telefono,
            )) {
                is Resultado.Ok -> _estado.update {
                    it.copy(cargando = false, usuario = resultado.datos)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    fun consumirSesion() {
        _estado.update { it.copy(usuario = null) }
    }

    private fun actualizar(transformacion: (RegisterUiState) -> RegisterUiState) {
        _estado.update { transformacion(it).copy(error = null) }
    }

    companion object {
        fun factory(usuarios: UsuarioRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RegisterViewModel(usuarios) as T
            }
    }
}
