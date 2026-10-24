package com.shoppi.data.repository

import com.shoppi.data.local.VentaDao
import com.shoppi.data.model.MetodoPago
import com.shoppi.data.model.Venta
import com.shoppi.data.model.VentaItem
import com.shoppi.util.ErrorVenta
import com.shoppi.util.SessionManager
import com.shoppi.util.VentaCalculator
import com.shoppi.util.VentaValidators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID

/**
 * Repositorio del punto de venta.
 *
 * La venta se guarda completa en Room (líneas como JSON) y se empuja después a
 * `POST /api/ventas`. El backend es quien descuenta el stock de forma
 * transaccional; el cliente no vuelve a descontarlo para no duplicar el efecto
 * cuando la venta se sincroniza.
 */
class VentaRepository(
    private val dao: VentaDao,
    private val session: SessionManager,
    private val ahora: () -> Long = System::currentTimeMillis,
    private val alCambiar: () -> Unit = {}
) {

    fun observar(): Flow<List<Venta>> =
        session.idUsuario()?.let { dao.observar(it) } ?: emptyFlow()

    suspend fun registrar(
        clienteId: String?,
        clienteNombre: String,
        items: List<VentaItem>,
        metodoPago: MetodoPago = MetodoPago.EFECTIVO,
        notas: String = ""
    ): Resultado<Venta> {
        val usuarioId = session.idUsuario()
            ?: return Resultado.Error("No hay una sesión activa.")

        val errores = VentaValidators.errores(items)
        if (errores.isNotEmpty()) {
            return Resultado.Error(
                when {
                    errores.contains(ErrorVenta.SIN_ITEMS) ->
                        "La venta debe tener al menos un producto"
                    else -> "Hay líneas con cantidad o precio inválidos"
                }
            )
        }

        val totales = VentaCalculator.calcularTotales(items)
        val instante = ahora()

        val venta = Venta(
            id = UUID.randomUUID().toString(),
            usuarioId = usuarioId,
            clienteId = clienteId,
            clienteNombre = clienteNombre.trim(),
            items = items,
            subtotal = totales.subtotal,
            impuestos = totales.impuesto,
            total = totales.total,
            metodoPago = metodoPago,
            notas = notas.trim(),
            fecha = instante,
            updatedAt = instante
        )

        dao.insertar(venta)
        alCambiar()
        return Resultado.Ok(venta)
    }

    suspend fun porId(id: String): Venta? = dao.porId(id)
}
