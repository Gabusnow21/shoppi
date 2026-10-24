package com.shoppi.sync

import com.shoppi.data.local.ClienteDao
import com.shoppi.data.local.ProductoDao
import com.shoppi.data.local.VentaDao
import com.shoppi.data.model.SyncState
import com.shoppi.data.remote.RemoteDataSource
import com.shoppi.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Motor de sincronización bidireccional entre Room y la API REST.
 *
 * Orden de operaciones:
 * 1. push de productos pendientes (para que la venta pueda resolver sus ids);
 * 2. push de clientes pendientes;
 * 3. push de ventas pendientes;
 * 4. pull de productos, clientes y ventas, resolviendo con [SyncPolicy].
 *
 * Resiliencia:
 * - Sin red o sin JWT: no se marca nada como `FAILED`; los registros siguen
 *   `PENDING` y se reintentan con WorkManager o con el botón manual.
 * - Error de red (IOException, timeout, 5xx, 429): se aborta el push y se deja
 *   `PENDING` (no se pierde el trabajo del usuario).
 * - Error de validación (400/401/403/404/422): el registro pasa a `FAILED` y el
 *   resto continúa.
 *
 * La sincronización se dispara tras cada mutación (one-shot) y cada 15 minutos
 * con [SyncScheduler]; además puede invocarse manualmente.
 */
class SyncManager(
    private val productos: ProductoDao,
    private val clientes: ClienteDao,
    private val ventas: VentaDao,
    private val session: SessionManager,
    private val remote: RemoteDataSource,
    private val hayRed: () -> Boolean = { true },
    private val ahora: () -> Long = System::currentTimeMillis
) {

    suspend fun sincronizarTodo(): ResultadoSync = withContext(Dispatchers.IO) {
        val usuarioId = session.idUsuario()
            ?: return@withContext ResultadoSync(
                mensaje = "No hay una sesión activa. No se sincronizó."
            )

        if (!hayRed()) {
            return@withContext ResultadoSync(
                sinConexion = true,
                mensaje = "Sin conexión: los cambios quedan pendientes."
            )
        }

        if (session.token().isNullOrBlank()) {
            return@withContext ResultadoSync(
                mensaje = "Iniciá sesión con internet para sincronizar."
            )
        }

        val conteo = Conteo()

        empujarProductos(usuarioId, conteo)
        if (!conteo.sinConexion) empujarClientes(usuarioId, conteo)
        if (!conteo.sinConexion) empujarVentas(usuarioId, conteo)

        if (!conteo.sinConexion) jalarProductos(usuarioId, conteo)
        if (!conteo.sinConexion) jalarClientes(usuarioId, conteo)
        if (!conteo.sinConexion) jalarVentas(usuarioId, conteo)

        ResultadoSync(
            enviados = conteo.enviados,
            recibidos = conteo.recibidos,
            fallidos = conteo.fallidos,
            sinConexion = conteo.sinConexion,
            mensaje = conteo.mensaje()
        )
    }

    // ------------------------------------------------------------------ push

    private suspend fun empujarProductos(usuarioId: String, conteo: Conteo) {
        for (producto in productos.pendientes(usuarioId)) {
            try {
                when {
                    producto.deleted -> {
                        producto.remoteId?.let { remote.eliminarProducto(it) }
                        productos.eliminarFisico(producto.id)
                    }

                    producto.remoteId == null -> {
                        val creado = remote.crearProducto(producto.toRequest())
                        productos.marcarSincronizado(producto.id, creado.id, ahora())
                    }

                    else -> {
                        remote.actualizarProducto(producto.remoteId, producto.toRequest())
                        productos.marcarSincronizado(producto.id, producto.remoteId, ahora())
                    }
                }
                conteo.enviados++
            } catch (error: Exception) {
                if (debeMarcarFallido(error, conteo)) {
                    productos.marcarSync(producto.id, SyncState.FAILED)
                    conteo.fallidos++
                }
                if (conteo.sinConexion) return
            }
        }
    }

    private suspend fun empujarClientes(usuarioId: String, conteo: Conteo) {
        for (cliente in clientes.pendientes(usuarioId)) {
            try {
                when {
                    cliente.deleted -> {
                        cliente.remoteId?.let { remote.eliminarCliente(it) }
                        clientes.eliminarFisico(cliente.id)
                    }

                    cliente.remoteId == null -> {
                        val creado = remote.crearCliente(cliente.toRequest())
                        clientes.marcarSincronizado(cliente.id, creado.id, ahora())
                    }

                    else -> {
                        remote.actualizarCliente(cliente.remoteId, cliente.toRequest())
                        clientes.marcarSincronizado(cliente.id, cliente.remoteId, ahora())
                    }
                }
                conteo.enviados++
            } catch (error: Exception) {
                if (debeMarcarFallido(error, conteo)) {
                    clientes.marcarSync(cliente.id, SyncState.FAILED)
                    conteo.fallidos++
                }
                if (conteo.sinConexion) return
            }
        }
    }

    private suspend fun empujarVentas(usuarioId: String, conteo: Conteo) {
        val mapaProductos = productos.todos(usuarioId).associate { it.id to it.remoteId }
        val mapaClientes = clientes.todos(usuarioId).associate { it.id to it.remoteId }

        for (venta in ventas.pendientes(usuarioId)) {
            // La API solo expone POST /api/ventas: una venta ya sincronizada no
            // vuelve a empujarse.
            if (venta.remoteId != null) continue

            try {
                val creada = remote.crearVenta(
                    venta.toRequest(
                        clienteRemoteId = venta.clienteId?.let { mapaClientes[it] },
                        productoRemoteId = { idLocal -> idLocal?.let { mapaProductos[it] } }
                    )
                )
                ventas.marcarSincronizado(venta.id, creada.id, ahora())
                conteo.enviados++
            } catch (error: Exception) {
                if (debeMarcarFallido(error, conteo)) {
                    ventas.marcarSync(venta.id, SyncState.FAILED)
                    conteo.fallidos++
                }
                if (conteo.sinConexion) return
            }
        }
    }

    // ------------------------------------------------------------------- pull

    private suspend fun jalarProductos(usuarioId: String, conteo: Conteo) {
        try {
            for (remoto in remote.listarProductos()) {
                val mapeado = remoto.toLocal(usuarioId, ahora = ahora())
                val local = productos.porRemoteId(usuarioId, remoto.id)
                val ganador = SyncPolicy.resolverProducto(local, mapeado) ?: continue
                productos.guardarDesdeSincronizacion(ganador)
                conteo.recibidos++
            }
        } catch (error: Exception) {
            if (debeMarcarFallido(error, conteo)) conteo.fallidos++
        }
    }

    private suspend fun jalarClientes(usuarioId: String, conteo: Conteo) {
        try {
            for (remoto in remote.listarClientes()) {
                val mapeado = remoto.toLocal(usuarioId, ahora = ahora())
                val local = clientes.porRemoteId(usuarioId, remoto.id)
                val ganador = SyncPolicy.resolverCliente(local, mapeado) ?: continue
                clientes.guardarDesdeSincronizacion(ganador)
                conteo.recibidos++
            }
        } catch (error: Exception) {
            if (debeMarcarFallido(error, conteo)) conteo.fallidos++
        }
    }

    private suspend fun jalarVentas(usuarioId: String, conteo: Conteo) {
        try {
            val mapaProductos = productos.todos(usuarioId)
                .mapNotNull { producto ->
                    producto.remoteId?.let {
                        it to ProductoRemotoLocal(producto.id, producto.nombre)
                    }
                }
                .toMap()

            val mapaClientes = clientes.todos(usuarioId)
                .mapNotNull { cliente ->
                    cliente.remoteId?.let {
                        it to ClienteRemotoLocal(cliente.id, cliente.nombre)
                    }
                }
                .toMap()

            for (remoto in remote.listarVentas()) {
                val mapeado = remoto.toLocal(
                    usuarioId = usuarioId,
                    clienteLocal = { mapaClientes[it] },
                    productoLocal = { mapaProductos[it] },
                    ahora = ahora()
                )
                val local = ventas.porRemoteId(usuarioId, remoto.id)
                val ganador = SyncPolicy.resolverVenta(local, mapeado) ?: continue
                ventas.guardarDesdeSincronizacion(ganador)
                conteo.recibidos++
            }
        } catch (error: Exception) {
            if (debeMarcarFallido(error, conteo)) conteo.fallidos++
        }
    }

    // -------------------------------------------------------------- auxiliares

    /**
     * @return `true` si el error no es de red y el registro debe pasar a FAILED.
     *         Si es de red, marca [Conteo.sinConexion] y deja el registro PENDING.
     */
    private fun debeMarcarFallido(error: Throwable, conteo: Conteo): Boolean {
        if (esErrorDeRed(error)) {
            conteo.sinConexion = true
            return false
        }
        return true
    }

    private class Conteo {
        var enviados = 0
        var recibidos = 0
        var fallidos = 0
        var sinConexion = false

        fun mensaje(): String = when {
            sinConexion -> "Sin conexión: los cambios quedan pendientes."
            fallidos > 0 -> "Sincronización parcial: $fallidos registro(s) con error."
            enviados == 0 && recibidos == 0 -> "Todo estaba al día."
            else -> "Sincronización completa: $enviados enviados, $recibidos recibidos."
        }
    }
}
