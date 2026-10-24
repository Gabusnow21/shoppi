package com.shoppi.data.remote

import com.shoppi.data.remote.dto.AuthResponse
import com.shoppi.data.remote.dto.ClienteRequest
import com.shoppi.data.remote.dto.ClienteResponse
import com.shoppi.data.remote.dto.LoginRequest
import com.shoppi.data.remote.dto.ProductoRequest
import com.shoppi.data.remote.dto.ProductoResponse
import com.shoppi.data.remote.dto.RegisterRequest
import com.shoppi.data.remote.dto.VentaRequest
import com.shoppi.data.remote.dto.VentaResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Contrato Retrofit de la API REST de Shoppi.
 *
 * Los endpoints siguen `docs/api_specification.md`. Este proyecto es la
 * implementación del **DAO remoto**: la app nunca escribe directo en PostgreSQL.
 */
interface ApiService {

    @POST("auth/register")
    suspend fun registrar(@Body request: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun iniciarSesion(@Body request: LoginRequest): AuthResponse

    @GET("productos")
    suspend fun productos(): List<ProductoResponse>

    @POST("productos")
    suspend fun crearProducto(@Body request: ProductoRequest): ProductoResponse

    @PUT("productos/{id}")
    suspend fun actualizarProducto(
        @Path("id") id: Long,
        @Body request: ProductoRequest
    ): ProductoResponse

    @DELETE("productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Long)

    @GET("clientes")
    suspend fun clientes(): List<ClienteResponse>

    @POST("clientes")
    suspend fun crearCliente(@Body request: ClienteRequest): ClienteResponse

    @PUT("clientes/{id}")
    suspend fun actualizarCliente(
        @Path("id") id: Long,
        @Body request: ClienteRequest
    ): ClienteResponse

    @DELETE("clientes/{id}")
    suspend fun eliminarCliente(@Path("id") id: Long)

    @GET("ventas")
    suspend fun ventas(): List<VentaResponse>

    @POST("ventas")
    suspend fun crearVenta(@Body request: VentaRequest): VentaResponse
}
