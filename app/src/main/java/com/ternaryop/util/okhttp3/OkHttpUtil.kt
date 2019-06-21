package com.ternaryop.util.okhttp3

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * OkHttp util class
 */
object OkHttpUtil {
    fun debugHttpClient(): OkHttpClient {
        val debugInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder().apply { interceptors().add(debugInterceptor) }.build()
    }
}