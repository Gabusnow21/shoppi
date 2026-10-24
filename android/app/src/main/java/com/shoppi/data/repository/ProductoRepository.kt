package com.shoppi.data.repository

import com.shoppi.data.local.ProductoDao
import com.shoppi.data.model.Producto
import com.shoppi.data.model.SyncState
import com.shoppi.util.ErrorProducto
import com.shoppi.util.ProductoValidators
import com.shoppi.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

/**
 * Repositorio de productos con estrategia offline-first.
 *
 * Toda escritura va primero a Room con `syncState = PENDING`; después se pide
 * una sincronización one-shot. La UI nunca espera a la red.
 */
class ProductoRepository(
    private val dao: ProductoDao,
    private val session: SessionManager,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val alCambiar: () -> Unit = {}
) {

    fun observar(): Flow<List<Producto>> =
        session.idUsuario()?.let { dao.observar(it) } ?: emptyFlow()

    suspend fun crear(
        nombre: String,
        descripcion: String = "",
        precio: Double,
        stockActual: Int,
        stockMinimo: Int,
        categoria: String = "",
        imagenUrl: String? = null
    ): Resultado<Producto> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")

        val errores = ProductoValidators.errores(nombre, precio, stockActual, stockMinimo)
        if (errores.isNotEmpty()) return Resultado.Error(mensaje(errores))

        val producto = Producto(
            id = UUID.randomUUID().toString(),
            usuarioId = usuarioId,
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            precio = precio,
            stockActual = stockActual,
            stockMinimo = stockMinimo,
            categoria = categoria.trim(),
            imagenUrl = imagenUrl,
            createdAt = ahora(),
            updatedAt = ahora()
        )

        dao.insertar(producto)
        alCambiar()
        return Resultado.Ok(producto)
    }

    suspend fun actualizar(producto: Producto): Resultado<Unit> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")
        if (producto.usuarioId != usuarioId) {
            return Resultado.Error("El producto no pertenece al usuario de la sesión.")
        }

        val errores = ProductoValidators.errores(
            nombre = producto.nombre,
            precio = producto.precio,
            stockActual = producto.stockActual,
            stockMinimo = producto.stockMinimo
        )
        if (errores.isNotEmpty()) return Resultado.Error(mensaje(errores))

        dao.actualizar(
            producto.copy(updatedAt = ahora(), syncState = SyncState.PENDING)
        )
        alCambiar()
        return Resultado.Ok(Unit)
    }

    suspend fun eliminar(id: String): Resultado<Unit> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")
        val producto = dao.porId(id)
            ?: return Resultado.Error("El producto no existe.")
        if (producto.usuarioId != usuarioId) {
            return Resultado.Error("El producto no pertenece al usuario de la sesión.")
        }

        dao.marcarBorrado(id, ahora())
        alCambiar()
        return Resultado.Ok(Unit)
    }

    private fun mensaje(errores: Set<ErrorProducto>): String =
        errores.joinToString(". ") { error ->
            when (error) {
                ErrorProducto.NOMBRE_CORTO -> "El nombre es demasiado corto"
                ErrorProducto.PRECIO_INVALIDO -> "El precio debe ser mayor o igual a 0"
                ErrorProducto.STOCK_INVALIDO -> "El stock debe ser mayor o igual a 0"
                ErrorProducto.STOCK_MINIMO_INVALIDO ->
                    "El stock mínimo debe ser mayor o igual a 0"
            }
        }
}
