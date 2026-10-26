package com.shoppi.data.repository

import com.shoppi.data.local.InventarioDao
import com.shoppi.data.local.ProductoDao
import com.shoppi.data.model.MovimientoInventario
import com.shoppi.data.model.TipoMovimiento
import com.shoppi.util.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

/**
 * Repositorio de movimientos de inventario, con el mismo patrón offline-first
 * que los demás: la escritura va primero a Room con `syncState = PENDING` y
 * `SyncManager` la empuja a `POST /api/inventario/movimiento`.
 *
 * El stock **no** se descuenta ni se suma en el cliente: lo hace el backend en
 * la misma transacción que guarda el movimiento, igual que con las ventas. Así
 * no se duplica el efecto cuando el movimiento se sincroniza.
 */
class InventarioRepository(
    private val dao: InventarioDao,
    private val productos: ProductoDao,
    private val session: SessionManager,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val alCambiar: () -> Unit = {}
) {

    /**
     * Historial filtrado. Todos los filtros son opcionales (`null` = sin
     * filtro) y los rangos de fecha van en epoch millis, ambos inclusive.
     */
    fun observar(
        productoId: String? = null,
        fechaInicio: Long? = null,
        fechaFin: Long? = null
    ): Flow<List<MovimientoInventario>> =
        session.idUsuario()
            ?.let { dao.observar(it, productoId, fechaInicio, fechaFin) }
            ?: emptyFlow()

    suspend fun porId(id: String): MovimientoInventario? = dao.porId(id)

    /**
     * Registra una entrada o salida para un producto propio.
     *
     * La validación de stock es un control local: el servidor vuelve a
     * validar y es quien aplica el cambio definitivo.
     */
    suspend fun registrar(
        productoId: String,
        tipoMovimiento: TipoMovimiento,
        cantidad: Int,
        descripcion: String = ""
    ): Resultado<MovimientoInventario> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")

        val producto = productos.porId(productoId)
            ?: return Resultado.Error("El producto no existe.")
        if (producto.usuarioId != usuarioId || producto.deleted) {
            return Resultado.Error("El producto no existe.")
        }
        if (producto.remoteId == null) {
            return Resultado.Error(
                "El producto todavía no está sincronizado. Esperá a que se " +
                    "actualice y volvé a intentar."
            )
        }
        if (cantidad <= 0) {
            return Resultado.Error("La cantidad debe ser mayor que 0.")
        }
        if (tipoMovimiento == TipoMovimiento.SALIDA && cantidad > producto.stockActual) {
            return Resultado.Error("Stock insuficiente. Disponible: ${producto.stockActual}.")
        }

        val movimiento = MovimientoInventario(
            id = UUID.randomUUID().toString(),
            usuarioId = usuarioId,
            productoId = producto.id,
            productoNombre = producto.nombre,
            tipoMovimiento = tipoMovimiento,
            cantidad = cantidad,
            fecha = ahora(),
            descripcion = descripcion.trim()
        )

        dao.insertar(movimiento)
        alCambiar()
        return Resultado.Ok(movimiento)
    }
}
