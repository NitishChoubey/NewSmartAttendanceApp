// RetrofitProvider.kt
package com.ebf.smartattendanceapp.data.net

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    // ⚠️ Keep this as your FRIEND’s laptop IP (Node server), NOT Mongo Atlas.
    // Example:
    private const val BASE_URL = "http://192.168.1.18:3001/" // must end with '/'

    private val bodyLogger = HttpLoggingInterceptor { msg ->
        Log.d("HTTP", msg)
    }.apply { level = HttpLoggingInterceptor.Level.BODY }

    private val headlineLogger = okhttp3.Interceptor { chain ->
        val req = chain.request()
        Log.d("HTTP", "--> ${req.method} ${req.url}")
        try {
            val res = chain.proceed(req)
            Log.d("HTTP", "<-- ${res.code} ${res.message} ${res.request.url}")
            res
        } catch (t: Throwable) {
            Log.e("HTTP", "!! HTTP error: ${t.message}", t)
            throw t
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headlineLogger)
            .addInterceptor(bodyLogger)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val attendanceApi: AttendanceApi by lazy {
        retrofit.create(AttendanceApi::class.java)
    }
}
