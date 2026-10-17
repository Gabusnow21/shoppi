package com.shoppi.data.remote

import com.shoppi.util.Constants
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Inyecta el header `Authorization: Bearer {token}` en cada request
 * (api_specification.md §Autenticación).
 *
 * El token se lee de forma diferida (`tokenProvider`) para no depender del orden
 * de inicialización; si no hay sesión, la request sale sin header (login/register).
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder()
                .header(Constants.AUTH_HEADER, Constants.BEARER_PREFIX + token)
                .build()
        }
        return chain.proceed(request)
    }
}
