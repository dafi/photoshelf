package com.ternaryop.photoshelf.util.network

import android.content.Context
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.birthday.BirthdayManager
import com.ternaryop.photoshelf.api.extractor.ImageExtractorManager
import com.ternaryop.photoshelf.api.post.PostManager

object ApiManager {
    private var imageExtractorManager: ImageExtractorManager? = null
    private var postManager: PostManager? = null
    private var birthdayManager: BirthdayManager? = null

    fun imageExtractorManager(context: Context): ImageExtractorManager {
        if (imageExtractorManager == null) {
            imageExtractorManager = ImageExtractorManager(context.getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN))
        }
        return imageExtractorManager!!
    }

    fun postManager(context: Context): PostManager {
        if (postManager == null) {
            postManager = PostManager(context.getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN))
        }
        return postManager!!
    }

    fun birthdayManager(context: Context): BirthdayManager {
        if (birthdayManager == null) {
            birthdayManager = BirthdayManager(context.getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN))
        }
        return birthdayManager!!
    }
}