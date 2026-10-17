package com.shoppi.data.remote

import android.content.Context
import com.shoppi.util.Constants
import com.shoppi.util.SessionManager
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente Retrofit único (singleton) para toda la app.
 *
 * Debe inicializarse una vez en [com.shoppi.ShoppiApp.onCreate] con
 * `RetrofitClient.init(applicationContext)` para que el interceptor pueda leer
 * el JWT de [SessionManager].
 */
object RetrofitClient {

    @Volatile
    private var sessionManager: SessionManager? = null

    /** Inicializa la sesión usada por el interceptor. Idempotente. */
    fun init(context: Context) {
        if (sessionManager == null) {
            synchronized(this) {
                if (sessionManager == null) {
                    sessionManager = SessionManager(context.applicationContext)
                }
            }
        }
    }

    /** SessionManager compartido (null si aún no se llamó a [init]). */
    fun session(): SessionManager? = sessionManager

    /** Base URL efectiva; en FASE 3B vendrá de Remote Config (`api_base_url`). */
    private val baseUrl: String
        get() = Constants.BASE_URL

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { sessionManager?.getToken() })
            .connectTimeout(Constants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Instancia de la API REST. */
    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
