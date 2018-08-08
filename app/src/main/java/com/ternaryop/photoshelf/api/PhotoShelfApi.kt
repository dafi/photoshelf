package com.ternaryop.photoshelf.api

import com.google.gson.GsonBuilder
import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.photoshelf.util.gson.CalendarDeserializer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Calendar

class Response<T>(val response: T)

class PhotoShelfApi(private val accessToken: String) {
    inline fun <reified T> service() : T = builder.create(T::class.java)

    val builder: Retrofit by lazy {
        val gson = GsonBuilder()
            .registerTypeAdapter(Calendar::class.java, CalendarDeserializer())
            .create()
        val interceptor = Interceptor {
            chain: Interceptor.Chain -> {
            val newRequest = chain.request().newBuilder()
                .addHeader("PhotoShelf-Subscription-Key", accessToken).build()
            chain.proceed(newRequest)
            }()
        }

        val builder = OkHttpClient.Builder()
        builder.interceptors().add(interceptor)

        if (BuildConfig.DEBUG) {
            val debugInterceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                this.level = HttpLoggingInterceptor.Level.BODY
            }
            builder.interceptors().add(debugInterceptor)
        }

        Retrofit.Builder()
            .baseUrl(BuildConfig.PHOTOSHELF_API_PREFIX)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())) // async by default
            .client(builder.build())
            .build()
    }
}
