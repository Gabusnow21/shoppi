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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

    /**
     * Historial de movimientos de inventario.
     *
     * Todos los filtros son opcionales: un `null` se omite en la query string.
     * Las fechas viajan como `yyyy-MM-dd` (ISO local date).
     */
    @GET("inventario")
    suspend fun inventario(
        @Query("productoId") productoId: Long?,
        @Query("fechaInicio") fechaInicio: String?,
        @Query("fechaFin") fechaFin: String?
    ): List<MovimientoResponse>

    @POST("inventario/movimiento")
    suspend fun crearMovimiento(@Body request: MovimientoRequest): MovimientoResponse

    // -------------------------------------------------------------- reportes

    /** Ventas agrupadas por día dentro del rango del período. */
    @GET("reportes/ventas")
    suspend fun reporteVentas(
        @Query("periodo") periodo: String,
        @Query("fechaInicio") fechaInicio: String?,
        @Query("fechaFin") fechaFin: String?
    ): ReporteVentasResponse

    /** Ranking de productos más vendidos, opcionalmente acotado por fechas. */
    @GET("reportes/productos-mas-vendidos")
    suspend fun productosMasVendidos(
        @Query("limite") limite: Int,
        @Query("fechaInicio") fechaInicio: String?,
        @Query("fechaFin") fechaFin: String?
    ): List<ProductoMasVendidoResponse>

    /** Estado actual del inventario (stock, valor y productos bajo mínimo). */
    @GET("reportes/inventario")
    suspend fun reporteInventario(): ReporteInventarioResponse
}
