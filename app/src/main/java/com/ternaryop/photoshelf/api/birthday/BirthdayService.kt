package com.ternaryop.photoshelf.api.birthday

import com.ternaryop.photoshelf.api.Response
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * Created by dave on 01/04/17.
 * Birthday API Manager
 * Migrated to Retrofit API Service on 08/08/2018.
 */

interface BirthdayService {
    @GET("v1/birthday/name/get")
    fun getByName(@Query("name") name: String, @Query("searchIfNew") searchIfNew: Boolean)
        : Single<Response<NameResult>>

    @GET("v1/birthday/date/find")
    fun findByDate(@QueryMap params: Map<String, String>): Single<Response<BirthdayResult>>

    @GET("v1/birthday/date/sameday")
    fun findSameDay(@QueryMap params: Map<String, String>): Single<Response<BirthdayResult>>

    @GET("v1/birthday/date/ignored")
    fun findIgnored(@QueryMap params: Map<String, String>): Single<Response<ListResult>>

    @GET("v1/birthday/date/orphans")
    fun findOrphans(@QueryMap params: Map<String, String>): Single<Response<BirthdayResult>>

    @GET("v1/birthday/name/missing")
    fun findMissingNames(@QueryMap params: Map<String, String>): Single<Response<ListResult>>

    @DELETE("v1/birthday/name")
    fun deleteByName(@Query("name") name: String): Completable

    @PUT("v1/birthday/date/edit")
    fun updateByName(@Query("name") name: String, @Query("birthdate") isoBirthdate: String): Completable

    @PUT("v1/birthday/name/ignore")
    fun markAsIgnored(@Query("name") name: String): Completable

    companion object {
        const val MAX_BIRTHDAY_COUNT = 200
    }
}
