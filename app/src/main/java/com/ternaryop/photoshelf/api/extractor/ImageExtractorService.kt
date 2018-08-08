package com.ternaryop.photoshelf.api.extractor

import com.ternaryop.photoshelf.api.Response
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by dave on 01/04/17.
 * Image Extractor Manager
 * Migrated to Retrofit API Service on 08/08/2018.
 */

interface ImageExtractorService {
    @GET("v1/extract/gallery")
    fun getGallery(@Query("url") url: String): Single<Response<ImageGalleryResult>>

    @GET("v1/extract/image")
    fun getImageUrl(@Query("url") url: String): Single<Response<ImageResult>>
}
