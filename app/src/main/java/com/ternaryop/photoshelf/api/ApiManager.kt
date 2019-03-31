package com.ternaryop.photoshelf.api

import com.ternaryop.photoshelf.api.birthday.BirthdayService
import com.ternaryop.photoshelf.api.extractor.ImageExtractorService
import com.ternaryop.photoshelf.api.parser.ParserService
import com.ternaryop.photoshelf.api.post.PostService

object ApiManager {
    private var photoShelfApi: PhotoShelfApi? = null
    private var accessToken = ""

    fun imageExtractorService(): ImageExtractorService = photoShelfApi().service()
    fun birthdayService(): BirthdayService = photoShelfApi().service()
    fun postService(): PostService = photoShelfApi().service()
    fun parserService(): ParserService = photoShelfApi().service()

    fun setup(accessToken: String) {
        this.accessToken = accessToken
    }

    private fun photoShelfApi(): PhotoShelfApi {
        if (photoShelfApi == null) {
            photoShelfApi = PhotoShelfApi(accessToken)
        }
        return photoShelfApi!!
    }
}
