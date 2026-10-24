package com.shoppi.data.repository

import com.shoppi.data.local.UsuarioDao
import com.shoppi.data.model.Usuario
import com.shoppi.data.remote.RemoteDataSource
import com.shoppi.data.remote.dto.AuthResponse
import com.shoppi.data.remote.dto.LoginRequest
import com.shoppi.data.remote.dto.RegisterRequest
import com.shoppi.sync.esErrorDeRed
import com.shoppi.util.PasswordHasher
import com.shoppi.util.SessionManager
import com.shoppi.util.Validators

/**
 * Autenticación con JWT propio y respaldo offline.
 *
 * - Online: `POST /api/auth/login` o `/register`; la respuesta trae el JWT.
 * - Sin red: se valida la contraseña contra el hash + salt guardados en Room
 *   (solo si el usuario ya inició sesión antes en este dispositivo).
 *
 * Un login offline deja el token vacío: se puede consultar y operar en local,
 * pero la sincronización exigirá volver a iniciar sesión con red.
 */
class UsuarioRepository(
    private val dao: UsuarioDao,
    private val remote: RemoteDataSource,
    private val session: SessionManager,
    private val ahora: () -> Long = System::currentTimeMillis
) {

    suspend fun registrar(
        nombre: String,
        email: String,
        password: String,
        telefono: String = "",
        recordar: Boolean = true
    ): Resultado<Usuario> {
        if (!Validators.nombreCompletoValido(nombre)) {
            return Resultado.Error("El nombre completo debe tener al menos 7 caracteres.")
        }
        if (!Validators.emailValido(email)) {
            return Resultado.Error("El correo no tiene un formato válido.")
        }
        if (!Validators.contrasenaValida(password)) {
            return Resultado.Error("La contraseña debe ser alfanumérica y tener al menos 8 caracteres.")
        }

        return try {
            val respuesta = remote.registrar(
                RegisterRequest(
                    nombre = nombre.trim(),
                    email = email.trim(),
                    password = password,
                    telefono = telefono.trim().ifBlank { null }
                )
            )
            persistirSesion(respuesta, email, password, recordar)
        } catch (error: Exception) {
            Resultado.Error(mensajeDeError(error, "No se pudo completar el registro."), error)
        }
    }

    suspend fun iniciarSesion(
        email: String,
        password: String,
        recordar: Boolean = true
    ): Resultado<Usuario> {
        if (!Validators.emailValido(email)) {
            return Resultado.Error("El correo no tiene un formato válido.")
        }
        if (password.isBlank()) {
            return Resultado.Error("Ingresá tu contraseña.")
        }

        return try {
            val respuesta = remote.iniciarSesion(LoginRequest(email.trim(), password))
            persistirSesion(respuesta, email, password, recordar)
        } catch (error: Exception) {
            if (!esErrorDeRed(error)) {
                return Resultado.Error("Credenciales incorrectas o usuario inexistente.", error)
            }
            iniciarSesionOffline(email, password, recordar)
        }
    }

    fun cerrarSesion() {
        session.cerrarSesion()
    }

    private suspend fun iniciarSesionOffline(
        email: String,
        password: String,
        recordar: Boolean
    ): Resultado<Usuario> {
        val local = dao.porEmail(email.trim())
            ?: return Resultado.Error(
                "Sin conexión y no hay una sesión guardada en este dispositivo."
            )

        if (!PasswordHasher.verificar(password, local.salt, local.passwordHash)) {
            return Resultado.Error("Contraseña incorrecta.")
        }

        session.guardarSesion(
            usuario = local,
            token = session.token().orEmpty(),
            recordar = recordar
        )
        return Resultado.Ok(local)
    }

    private suspend fun persistirSesion(
        respuesta: AuthResponse,
        emailIngresado: String,
        password: String,
        recordar: Boolean
    ): Resultado<Usuario> {
        if (respuesta.token.isBlank()) {
            return Resultado.Error("El servidor no devolvió un token válido.")
        }

        val salt = PasswordHasher.nuevaSalt()
        val existente = dao.porEmail(respuesta.email.ifBlank { emailIngresado.trim() })
        val instante = ahora()
        val usuario = Usuario(
            id = respuesta.id.toString(),
            email = respuesta.email.ifBlank { emailIngresado.trim() },
            nombreCompleto = respuesta.nombre.ifBlank { emailIngresado.substringBefore("@") },
            telefono = existente?.telefono.orEmpty(),
            passwordHash = PasswordHasher.hash(password, salt),
            salt = salt,
            createdAt = existente?.createdAt ?: instante,
            updatedAt = instante
        )

        dao.guardar(usuario)
        session.guardarSesion(usuario, respuesta.token, recordar)
        return Resultado.Ok(usuario)
    }

    private fun mensajeDeError(error: Throwable, predeterminado: String): String =
        if (esErrorDeRed(error)) {
            "Sin conexión: para registrarte o iniciar sesión por primera vez necesitás internet."
        } else {
            predeterminado
        }
}
