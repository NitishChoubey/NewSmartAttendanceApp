package com.ebf.smartattendanceapp.data.net

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    // ⚠️ Use your FRIEND'S laptop Wi-Fi IPv4 + port 3001 + trailing slash
    private const val BASE_URL = "http://192.168.1.18:3001/"

    // Custom BODY logger so you can filter Logcat by tag "HTTP"
    private val bodyLogger = HttpLoggingInterceptor { msg ->
        Log.d("HTTP", msg)
    }.apply { level = HttpLoggingInterceptor.Level.BODY }

    // Simple headline logger (helps even if BODY is trimmed)
    private val headlineLogger = okhttp3.Interceptor { chain ->
        val req = chain.request()
        Log.d("HTTP", "--> ${req.method} ${req.url}")
        val res = chain.proceed(req)
        Log.d("HTTP", "<-- ${res.code} ${res.message} ${res.request.url}")
        res
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headlineLogger)
            .addInterceptor(bodyLogger)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // must end with /
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    val attendanceApi: AttendanceApi by lazy {
        retrofit.create(AttendanceApi::class.java)
    }
}
