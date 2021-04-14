package com.ternaryop.photoshelf.activity

import android.content.Context
import android.content.Intent
import java.io.Serializable

class TagPhotoBrowserData(
    val blogName: String?,
    val tag: String,
    val allowSearch: Boolean
) : Serializable

class ImageViewerData(
    val imageUrl: String,
    val title: String? = null,
    val tag: String? = null
) : Serializable

interface ImageViewerActivityStarter {
    fun startImagePicker(context: Context, url: String)
    fun startImagePickerPrefetch(urls: List<String>)

    /**
     * Prepare the intent need to launch the tag photo browser activity
     * @param context the context
     * @param tagPhotoBrowserData the data used by activity
     * @param returnSelectedPost true if selected item must be returned, false otherwise
     */
    fun tagPhotoBrowserIntent(
        context: Context,
        tagPhotoBrowserData: TagPhotoBrowserData,
        returnSelectedPost: Boolean = false
    ): Intent

    fun startImageViewer(context: Context, data: ImageViewerData)
}
