package com.ternaryop.photoshelf.customsearch

import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by dave on 30/04/17.
 * Mini Client interface for Google Custom Search (CSE)
 * https://cse.google.com/cse/all
 * https://developers.google.com/custom-search/json-api/v1/using_rest
 * Migrated to Retrofit API Service on 10/08/2018.
 */

private const val API_PREFIX = "https://www.googleapis.com/"

interface GoogleCustomSearchService {
    @GET("customsearch/v1")
    suspend fun runCustomSearch(@Query("q") q: String, @Query("fields") fields: String): CustomSearchResult
}

object GoogleCustomSearchClient {
    private var apiKey = ""
    private var cx = ""
    private var okHttpClient: OkHttpClient? = null

    fun setup(apiKey: String, cx: String, okHttpClient: OkHttpClient?) {
        this.apiKey = apiKey
        this.cx = cx
        this.okHttpClient = okHttpClient
    }

    suspend fun getCorrectedQuery(q: String): CustomSearchResult {
        return service().runCustomSearch(q, arrayOf("spelling").joinToString(","))
    }

    private fun service(): GoogleCustomSearchService = builder.create(GoogleCustomSearchService::class.java)

    private val builder: Retrofit by lazy {
        val moshi = Moshi.Builder().build()
        val interceptor = Interceptor { chain: Interceptor.Chain ->
            val original = chain.request()
            val originalHttpUrl = original.url
            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("key", apiKey)
                .addQueryParameter("cx", cx)
                .build()
            chain.proceed(original.newBuilder().url(url).build())
        }

        val builder = okHttpClient?.newBuilder() ?: OkHttpClient.Builder()
        builder.interceptors().add(interceptor)

        Retrofit.Builder()
            .baseUrl(API_PREFIX)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(builder.build())
            .build()
    }
}
