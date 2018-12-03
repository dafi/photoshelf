package com.ternaryop.feedly

import com.dropbox.core.util.StringUtil.UTF8
import com.google.gson.GsonBuilder
import com.ternaryop.photoshelf.BuildConfig
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap
import java.net.HttpURLConnection

/**
 * Created by dave on 24/02/17.
 * Feedly Manager
 * Migrated to Retrofit API Service on 09/08/2018.
 */

private const val API_PREFIX = "https://cloud.feedly.com/"

interface FeedlyService {
    @GET("v3/streams/contents")
    fun getStreamContents(@Query("streamId") streamId: String, @QueryMap params: Map<String, String>)
        : Single<StreamContent>

    @FormUrlEncoded
    @POST("v3/auth/token")
    fun refreshAccessToken(@FieldMap params: Map<String, String>) : Single<AccessToken>

    @POST("v3/markers")
    fun markSaved(@Body marker: Marker) : Completable
}

class FeedlyClient(var accessToken: String, userId: String, private val refreshToken: String) {

    val globalSavedTag = "user/$userId/tag/global.saved"

    fun getStreamContents(streamId: String, params: Map<String, String>): Single<StreamContent> {
        return service()
            .getStreamContents(streamId, params)
    }

    fun markSaved(ids: List<String>, saved: Boolean): Completable {
        if (ids.isEmpty()) {
            return Completable.complete()
        }
        return service()
            .markSaved(Marker("entries", if (saved) "markAsSaved" else "markAsUnsaved", ids))
    }

    fun refreshAccessToken(clientId: String, clientSecret: String): Single<AccessToken> {
        val data = mapOf(
            "refresh_token" to refreshToken,
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "refresh_token"
        )
        return service()
            .refreshAccessToken(data)
    }

    fun service(): FeedlyService = builder.create(FeedlyService::class.java)

    val builder: Retrofit by lazy {
        val gson = GsonBuilder()
            .create()
        val authInterceptor = Interceptor { chain: Interceptor.Chain ->
            {
                val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "OAuth $accessToken").build()
                chain.proceed(newRequest)
            }()
        }
        val rateInterceptor = Interceptor { chain: Interceptor.Chain ->
            {
                val request = chain.request()
                val response = chain.proceed(request)

                FeedlyRateLimit.update(response.code(), response.headers())
                response
            }()
        }

        val builder = OkHttpClient.Builder()
        builder.interceptors().add(authInterceptor)
        builder.interceptors().add(errorInterceptor)
        builder.interceptors().add(rateInterceptor)

        if (BuildConfig.DEBUG) {
            val debugInterceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                this.level = HttpLoggingInterceptor.Level.BODY
            }
            builder.interceptors().add(debugInterceptor)
        }

        Retrofit.Builder()
            .baseUrl(API_PREFIX)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .client(builder.build())
            .build()
    }

    companion object {
        private val errorInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val responseCode = response.code()

            if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                response.body()?.source()?.also { source ->
                    source.request(java.lang.Long.MAX_VALUE) // Buffer the entire body.
                    val json = source.buffer().clone().readString(UTF8)
                    val error = GsonBuilder().create().fromJson<Error>(json, Error::class.java)
                    if (error.hasTokenExpired()) {
                        throw TokenExpiredException(error.errorMessage!!)
                    }
                    throw RuntimeException("Error $responseCode: ${error.errorMessage}")
                }
            }
            response
        }
    }
}