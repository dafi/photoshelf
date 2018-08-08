package com.ternaryop.photoshelf.api.post

import com.ternaryop.photoshelf.api.Response
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by dave on 14/06/2018.
 * Photo Post API Manager
 * Migrated to Retrofit API Service on 08/08/2018.
 */
interface PostService {
    @GET("v1/post/{blogName}/latestTimestamp")
    fun getLastPublishedTimestamp(@Path("blogName") blogName: String, @Query("since") since: Long)
        : Single<Response<LatestTimestampResult>>

    @GET("v1/post/{blogName}/stats")
    fun getStats(@Path("blogName") blogName: String): Single<Response<StatsResult>>

    @GET("v1/post/tag")
    fun getCorrectMisspelledName(@Query("misspelled") name: String): Single<Response<MisspelledResult>>

    @POST("v1/post/{blogName}/latestTag")
    fun getMapLastPublishedTimestampTag(@Path("blogName") blogName: String, @Body titles: RequestBody)
        : Single<Response<LatestTagResult>>

    @GET("v1/post/{blogName}/tags")
    fun findTags(@Path("blogName") blogName: String, @Query("t") pattern: String)
        : Single<Response<TagInfoListResult>>

    @FormUrlEncoded
    @POST("v1/post/editTags")
    fun editTags(@Field("postId") postId: Long, @Field("t[]") tags: List<String>) : Completable

    @DELETE("v1/post/{postId}")
    fun deletePost(@Path("postId") postId: Long): Completable
}

