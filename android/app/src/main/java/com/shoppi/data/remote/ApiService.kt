package com.shoppi.data.remote

/**
 * Contrato de la API REST (`api_specification.md`).
 *
 * En FASE 2.1 (T-103) se deja vacío a propósito: los endpoints se agregan en las
 * tareas 2.2+ (auth, productos, inventario, clientes, ventas, reportes).
 *
 * Convención: rutas relativas a `Constants.BASE_URL` (`.../api/`), p. ej.
 * `@POST("auth/login")`.
 */
interface ApiService {
    // TODO 2.2 (T-111): @POST("auth/login") / @POST("auth/register")
    // TODO 2.3 (T-121): @GET("productos") ...
}
