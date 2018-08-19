package com.ternaryop.photoshelf.api

import android.content.Context
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.birthday.BirthdayService
import com.ternaryop.photoshelf.api.extractor.ImageExtractorService
import com.ternaryop.photoshelf.api.post.PostService

object ApiManager {
    private var photoShelfApi: PhotoShelfApi? = null

    fun imageExtractorService(context: Context): ImageExtractorService = photoShelfApi(context).service()
    fun birthdayService(context: Context): BirthdayService = photoShelfApi(context).service()
    fun postService(context: Context): PostService = photoShelfApi(context).service()

    private fun photoShelfApi(context: Context): PhotoShelfApi {
        if (photoShelfApi == null) {
            photoShelfApi = PhotoShelfApi(context.getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN))
        }
        return photoShelfApi!!
    }
}
