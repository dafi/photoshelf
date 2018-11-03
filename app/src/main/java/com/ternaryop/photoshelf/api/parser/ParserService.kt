package com.ternaryop.photoshelf.api.parser

import com.ternaryop.photoshelf.api.Response
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface ParserService {
    @GET("v1/parser/components")
    fun components(
        @Query("title") title: String,
        @Query("swapDayMonth") swapDayMonth: Boolean = false,
        @Query("checkDateInTheFuture") checkDateInTheFuture: Boolean = true): Single<Response<TitleComponentsResult>>
}