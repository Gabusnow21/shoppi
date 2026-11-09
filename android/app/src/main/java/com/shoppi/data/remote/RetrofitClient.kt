package com.shoppi.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fábrica del cliente Retrofit.
 *
 * El JWT se inyecta como `Authorization: Bearer ...` en cada petición. La URL
 * base sale de `BuildConfig.API_BASE_URL`: en debug apunta a
 * `http://127.0.0.1:8080/api/` y se usa junto con `adb reverse` (ver
 * `scripts/prepare-dev.sh`), por lo que funciona en emulador y dispositivo
 * físico. Se puede sobreescribir con `shoppi.apiBaseUrl` en
 * `android/local.properties`.
 */
object RetrofitClient {

    const val BASE_URL_EMULADOR = "http://10.0.2.2:8080/api/"

    fun crear(
        token: () -> String?,
        baseUrl: String = com.shoppi.BuildConfig.API_BASE_URL
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
