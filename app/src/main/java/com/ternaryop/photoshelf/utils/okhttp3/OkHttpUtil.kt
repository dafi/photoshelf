package com.ternaryop.photoshelf.utils.okhttp3

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Return a builder with log interceptor when isDebugEnabled is true
 */
object OkHttpUtil {
    var isDebugEnabled = false

    fun Builder(): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()

        if (isDebugEnabled) {
            val debugInterceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                this.level = HttpLoggingInterceptor.Level.BODY
            }
            builder.interceptors().add(debugInterceptor)
        }
        return builder
    }

}