package com.ternaryop.photoshelf.util.image

import android.content.Context
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.ternaryop.photoshelf.R

@GlideModule
class PhotoShelfAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        builder.setDefaultRequestOptions(
            RequestOptions()
                .placeholder(R.drawable.stub)
                .error(R.drawable.ic_sync_problem_black_24dp)
        )
    }
}
