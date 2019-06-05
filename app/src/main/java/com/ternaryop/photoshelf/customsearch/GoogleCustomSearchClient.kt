package com.ternaryop.photoshelf.customsearch

import com.google.gson.GsonBuilder
import com.ternaryop.photoshelf.utils.okhttp3.OkHttpUtil
import io.reactivex.Single
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
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
    fun runCustomSearch(@Query("q") q: String, @Query("fields") fields: String): Single<CustomSearchResult>
}

object GoogleCustomSearchClient {
    private var apiKey = ""
    private var cx = ""

    fun setup(apiKey: String, cx: String) {
        this.apiKey = apiKey
        this.cx = cx
    }

    fun getCorrectedQuery(q: String): Single<CustomSearchResult> {
        return service().runCustomSearch(q, arrayOf("spelling").joinToString(","))
    }

    private fun service(): GoogleCustomSearchService = builder.create(GoogleCustomSearchService::class.java)

    private val builder: Retrofit by lazy {
        val gson = GsonBuilder()
            .create()
        val interceptor = Interceptor { chain: Interceptor.Chain -> {
                val original = chain.request()
                val originalHttpUrl = original.url
                val url = originalHttpUrl.newBuilder()
                    .addQueryParameter("key", apiKey)
                    .addQueryParameter("cx", cx)
                    .build()
                chain.proceed(original.newBuilder().url(url).build())
            }()
        }

        val builder = OkHttpUtil.Builder()
        builder.interceptors().add(interceptor)

        Retrofit.Builder()
            .baseUrl(API_PREFIX)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(builder.build())
            .build()
    }
}
