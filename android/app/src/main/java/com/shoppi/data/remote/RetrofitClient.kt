package com.shoppi.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fábrica del cliente Retrofit.
 *
 * El JWT se inyecta como `Authorization: Bearer ...` en cada petición. La URL
 * base por defecto apunta al backend local del emulador (`10.0.2.2`).
 */
object RetrofitClient {

    const val BASE_URL_EMULADOR = "http://10.0.2.2:8080/api/"

    fun crear(
        token: () -> String?,
        baseUrl: String = BASE_URL_EMULADOR
    ): ApiService {
        val cliente = OkHttpClient.Builder()
            .addInterceptor { cadena ->
                val jwt = token()
                val peticion = if (jwt.isNullOrBlank()) {
                    cadena.request()
                } else {
                    cadena.request().newBuilder()
                        .header("Authorization", "Bearer $jwt")
                        .build()
                }
                cadena.proceed(peticion)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
