package com.shoppi

import android.content.Context
import com.shoppi.data.local.AppDatabase
import com.shoppi.data.remote.ApiRemoteDataSource
import com.shoppi.data.remote.ApiService
import com.shoppi.data.remote.RemoteDataSource
import com.shoppi.data.remote.RetrofitClient
import com.shoppi.data.repository.ClienteRepository
import com.shoppi.data.repository.ProductoRepository
import com.shoppi.data.repository.UsuarioRepository
import com.shoppi.data.repository.VentaRepository
import com.shoppi.sync.NetworkMonitor
import com.shoppi.sync.SyncManager
import com.shoppi.sync.SyncScheduler
import com.shoppi.util.SessionManager

/**
 * Inyección de dependencias manual.
 *
 * Evita frameworks de DI y deja explícito el grafo: sesión → Room → API REST →
 * SyncManager → repositorios. Los repositorios disparan una sincronización
 * one-shot después de cada mutación.
 */
class AppContainer(contexto: Context) {

    private val appContext = contexto.applicationContext

    val session: SessionManager = SessionManager(appContext)

    val database: AppDatabase = AppDatabase.obtener(appContext)

    val networkMonitor: NetworkMonitor = NetworkMonitor(appContext)

    val api: ApiService = RetrofitClient.crear(token = { session.token() })

    val remote: RemoteDataSource = ApiRemoteDataSource(api)

    val syncManager: SyncManager = SyncManager(
        productos = database.productoDao(),
        clientes = database.clienteDao(),
        ventas = database.ventaDao(),
        inventario = database.inventarioDao(),
        session = session,
        remote = remote,
        hayRed = { networkMonitor.hayRed() }
    )

    val productos: ProductoRepository = ProductoRepository(
        dao = database.productoDao(),
        session = session,
        alCambiar = { SyncScheduler.sincronizarAhora(appContext) }
    )

    val clientes: ClienteRepository = ClienteRepository(
        dao = database.clienteDao(),
        session = session,
        alCambiar = { SyncScheduler.sincronizarAhora(appContext) }
    )

    val ventas: VentaRepository = VentaRepository(
        dao = database.ventaDao(),
        session = session,
        alCambiar = { SyncScheduler.sincronizarAhora(appContext) }
    )

    val usuarios: UsuarioRepository = UsuarioRepository(
        dao = database.usuarioDao(),
        remote = remote,
        session = session
    )

    fun programarSyncPeriodico() {
        SyncScheduler.programarPeriodico(appContext)
    }
}
