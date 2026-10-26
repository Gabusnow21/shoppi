package com.shoppi.data.remote

import com.shoppi.data.remote.dto.AuthResponse
import com.shoppi.data.remote.dto.ClienteRequest
import com.shoppi.data.remote.dto.ClienteResponse
import com.shoppi.data.remote.dto.LoginRequest
import com.shoppi.data.remote.dto.MovimientoRequest
import com.shoppi.data.remote.dto.MovimientoResponse
import com.shoppi.data.remote.dto.ProductoRequest
import com.shoppi.data.remote.dto.ProductoResponse
import com.shoppi.data.remote.dto.ProductoMasVendidoResponse
import com.shoppi.data.remote.dto.RegisterRequest
import com.shoppi.data.remote.dto.ReporteInventarioResponse
import com.shoppi.data.remote.dto.ReporteVentasResponse
import com.shoppi.data.remote.dto.VentaRequest
import com.shoppi.data.remote.dto.VentaResponse

/**
 * Abstracción del DAO remoto.
 *
 * En producción la implementa [ApiRemoteDataSource] (Retrofit); en pruebas se
 * sustituye por un doble en memoria. Así el `SyncManager` se puede verificar sin
 * red y sin Android.
 */
interface RemoteDataSource {

    suspend fun registrar(request: RegisterRequest): AuthResponse

    suspend fun iniciarSesion(request: LoginRequest): AuthResponse

    suspend fun listarProductos(): List<ProductoResponse>

    suspend fun crearProducto(request: ProductoRequest): ProductoResponse

    suspend fun actualizarProducto(id: Long, request: ProductoRequest): ProductoResponse

    suspend fun eliminarProducto(id: Long)

    suspend fun listarClientes(): List<ClienteResponse>

    suspend fun crearCliente(request: ClienteRequest): ClienteResponse

    suspend fun actualizarCliente(id: Long, request: ClienteRequest): ClienteResponse

    suspend fun eliminarCliente(id: Long)

    suspend fun listarVentas(): List<VentaResponse>

    suspend fun crearVenta(request: VentaRequest): VentaResponse

    /**
     * Historial de movimientos de inventario. Los filtros son opcionales
     * (`null` = sin filtro) y las fechas van como `yyyy-MM-dd`.
     */
    suspend fun listarInventario(
        productoId: Long?,
        fechaInicio: String?,
        fechaFin: String?
    ): List<MovimientoResponse>

    suspend fun crearMovimiento(request: MovimientoRequest): MovimientoResponse

    suspend fun reporteVentas(
        periodo: String,
        fechaInicio: String?,
        fechaFin: String?
    ): ReporteVentasResponse

    suspend fun productosMasVendidos(
        limite: Int,
        fechaInicio: String?,
        fechaFin: String?
    ): List<ProductoMasVendidoResponse>

    suspend fun reporteInventario(): ReporteInventarioResponse
}
