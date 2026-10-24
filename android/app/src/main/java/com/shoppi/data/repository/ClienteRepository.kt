package com.shoppi.data.repository

import com.shoppi.data.local.ClienteDao
import com.shoppi.data.model.Cliente
import com.shoppi.data.model.SyncState
import com.shoppi.util.ClienteValidators
import com.shoppi.util.ErrorCliente
import com.shoppi.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

/** Repositorio de clientes con el mismo patrón offline-first que ProductoRepository. */
class ClienteRepository(
    private val dao: ClienteDao,
    private val session: SessionManager,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val alCambiar: () -> Unit = {}
) {

    fun observar(): Flow<List<Cliente>> =
        session.idUsuario()?.let { dao.observar(it) } ?: emptyFlow()

    suspend fun crear(
        nombre: String,
        telefono: String = "",
        email: String = "",
        direccion: String = ""
    ): Resultado<Cliente> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")

        val errores = ClienteValidators.errores(nombre, telefono, email)
        if (errores.isNotEmpty()) return Resultado.Error(mensaje(errores))

        val cliente = Cliente(
            id = UUID.randomUUID().toString(),
            usuarioId = usuarioId,
            nombre = nombre.trim(),
            telefono = telefono.trim(),
            email = email.trim(),
            direccion = direccion.trim(),
            createdAt = ahora(),
            updatedAt = ahora()
        )

        dao.insertar(cliente)
        alCambiar()
        return Resultado.Ok(cliente)
    }

    suspend fun actualizar(cliente: Cliente): Resultado<Unit> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")
        if (cliente.usuarioId != usuarioId) {
            return Resultado.Error("El cliente no pertenece al usuario de la sesión.")
        }

        val errores = ClienteValidators.errores(cliente.nombre, cliente.telefono, cliente.email)
        if (errores.isNotEmpty()) return Resultado.Error(mensaje(errores))

        dao.actualizar(cliente.copy(updatedAt = ahora(), syncState = SyncState.PENDING))
        alCambiar()
        return Resultado.Ok(Unit)
    }

    suspend fun eliminar(id: String): Resultado<Unit> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")
        val cliente = dao.porId(id)
            ?: return Resultado.Error("El cliente no existe.")
        if (cliente.usuarioId != usuarioId) {
            return Resultado.Error("El cliente no pertenece al usuario de la sesión.")
        }

        dao.marcarBorrado(id, ahora())
        alCambiar()
        return Resultado.Ok(Unit)
    }

    private fun mensaje(errores: Set<ErrorCliente>): String =
        errores.joinToString(". ") { error ->
            when (error) {
                ErrorCliente.NOMBRE_CORTO -> "El nombre es demasiado corto"
                ErrorCliente.EMAIL_INVALIDO -> "El correo no tiene un formato válido"
                ErrorCliente.TELEFONO_INVALIDO -> "El teléfono no tiene un formato válido"
            }
        }
}
