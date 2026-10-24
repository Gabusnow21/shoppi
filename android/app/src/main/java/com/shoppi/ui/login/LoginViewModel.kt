package com.shoppi.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shoppi.data.model.Usuario
import com.shoppi.data.repository.Resultado
import com.shoppi.data.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado del formulario de login.
 *
 * `usuario` se emite una sola vez como señal de éxito; la pantalla lo consume y
 * navega al dashboard.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val recordarSesion: Boolean = true,
    val passwordVisible: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val usuario: Usuario? = null,
)

/**
 * ViewModel del login.
 *
 * Delega en [UsuarioRepository]: login online con JWT y respaldo offline si no
 * hay red y el usuario ya inició sesión antes en el dispositivo.
 */
class LoginViewModel(private val usuarios: UsuarioRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarEmail(valor: String) {
        _estado.update { it.copy(email = valor, error = null) }
    }

    fun cambiarPassword(valor: String) {
        _estado.update { it.copy(password = valor, error = null) }
    }

    fun cambiarRecordarSesion(valor: Boolean) {
        _estado.update { it.copy(recordarSesion = valor) }
    }

    fun alternarVisibilidadPassword() {
        _estado.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun iniciarSesion() {
        val actual = _estado.value
        if (actual.cargando) return

        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val resultado =
                usuarios.iniciarSesion(actual.email, actual.password, actual.recordarSesion)) {
                is Resultado.Ok -> _estado.update {
                    it.copy(cargando = false, usuario = resultado.datos)
                }

                is Resultado.Error -> _estado.update {
                    it.copy(cargando = false, error = resultado.mensaje)
                }
            }
        }
    }

    /** La pantalla ya navegó; evita reaccionar dos veces al mismo usuario. */
    fun consumirSesion() {
        _estado.update { it.copy(usuario = null) }
    }

    companion object {
        fun factory(usuarios: UsuarioRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LoginViewModel(usuarios) as T
            }
    }
}
