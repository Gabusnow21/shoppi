package com.shoppi.data.remote

import com.shoppi.data.remote.dto.AuthResponse
import com.shoppi.data.remote.dto.ClienteRequest
import com.shoppi.data.remote.dto.ClienteResponse
import com.shoppi.data.remote.dto.LoginRequest
import com.shoppi.data.remote.dto.MovimientoRequest
import com.shoppi.data.remote.dto.MovimientoResponse
import com.shoppi.data.remote.dto.ProductoRequest
import com.shoppi.data.remote.dto.ProductoResponse
import com.shoppi.data.remote.dto.RegisterRequest
import com.shoppi.data.remote.dto.VentaRequest
import com.shoppi.data.remote.dto.VentaResponse

/** Implementación de [RemoteDataSource] sobre Retrofit. */
class ApiRemoteDataSource(private val api: ApiService) : RemoteDataSource {

    override suspend fun registrar(request: RegisterRequest): AuthResponse = api.registrar(request)

    override suspend fun iniciarSesion(request: LoginRequest): AuthResponse = api.iniciarSesion(request)

    override suspend fun listarProductos(): List<ProductoResponse> = api.productos()

    override suspend fun crearProducto(request: ProductoRequest): ProductoResponse =
        api.crearProducto(request)

    override suspend fun actualizarProducto(id: Long, request: ProductoRequest): ProductoResponse =
        api.actualizarProducto(id, request)

    override suspend fun eliminarProducto(id: Long) = api.eliminarProducto(id)

    override suspend fun listarClientes(): List<ClienteResponse> = api.clientes()

    override suspend fun crearCliente(request: ClienteRequest): ClienteResponse =
        api.crearCliente(request)

    override suspend fun actualizarCliente(id: Long, request: ClienteRequest): ClienteResponse =
        api.actualizarCliente(id, request)

    override suspend fun eliminarCliente(id: Long) = api.eliminarCliente(id)

    override suspend fun listarVentas(): List<VentaResponse> = api.ventas()

    override suspend fun crearVenta(request: VentaRequest): VentaResponse = api.crearVenta(request)

    override suspend fun listarInventario(
        productoId: Long?,
        fechaInicio: String?,
        fechaFin: String?
    ): List<MovimientoResponse> = api.inventario(productoId, fechaInicio, fechaFin)

    override suspend fun crearMovimiento(request: MovimientoRequest): MovimientoResponse =
        api.crearMovimiento(request)
}
