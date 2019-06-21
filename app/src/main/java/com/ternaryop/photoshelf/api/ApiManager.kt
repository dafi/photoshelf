package com.ternaryop.photoshelf.api

import com.ternaryop.photoshelf.api.birthday.BirthdayService
import com.ternaryop.photoshelf.api.extractor.ImageExtractorService
import com.ternaryop.photoshelf.api.parser.ParserService
import com.ternaryop.photoshelf.api.post.PostService
import okhttp3.OkHttpClient

object ApiManager {
    private var photoShelfApi: PhotoShelfApi? = null
    private var accessToken = ""
    private var apiUrlPrefix = ""
    private var okHttpClient: OkHttpClient? = null

    fun imageExtractorService(): ImageExtractorService = photoShelfApi().service()
    fun birthdayService(): BirthdayService = photoShelfApi().service()
    fun postService(): PostService = photoShelfApi().service()
    fun parserService(): ParserService = photoShelfApi().service()

    fun setup(accessToken: String, apiUrlPrefix: String, okHttpClient: OkHttpClient?) {
        this.accessToken = accessToken
        this.apiUrlPrefix = apiUrlPrefix
        this.photoShelfApi = null
        this.okHttpClient = okHttpClient
    }

    fun updateToken(accessToken: String) = setup(accessToken, apiUrlPrefix, okHttpClient)

    private fun photoShelfApi(): PhotoShelfApi {
        if (photoShelfApi == null) {
            photoShelfApi = PhotoShelfApi(accessToken, apiUrlPrefix, okHttpClient)
        }
        return photoShelfApi!!
    }
}
