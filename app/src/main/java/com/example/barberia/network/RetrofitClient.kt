package com.example.barberia.network

import com.example.barberia.utils.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://backendbarberia-ga9f.onrender.com/"

    // Se inicializa una sola vez cuando se necesita por primera vez
    private var sessionManager: SessionManager? = null

    fun init(manager: SessionManager) {
        sessionManager = manager
    }

    // Interceptor JWT — agrega "Authorization: Bearer <token>" a cada request
    // Si no hay token (usuario no logueado) simplemente no agrega el header
    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking {
            sessionManager?.token?.firstOrNull()
        }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        chain.proceed(request)
    }

    // Interceptor de logs — muestra en Logcat cada request y response
    // Muy útil para depurar qué se envía y qué responde el backend
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        // 90 segundos porque Render en plan gratuito tarda 30-60s en despertar
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(
            GsonBuilder().setLenient().disableHtmlEscaping().create()))
        .build()

    // Instancia única de ApiService — se crea una sola vez
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}