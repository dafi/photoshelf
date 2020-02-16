package com.ternaryop.photoshelf.activity

import android.content.Context
import androidx.fragment.app.Fragment
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
    fun startTagPhotoBrowser(context: Context, data: TagPhotoBrowserData)
    fun startTagPhotoBrowserForResult(fragment: Fragment, requestCode: Int, data: TagPhotoBrowserData)
    fun startImageViewer(context: Context, data: ImageViewerData)
}
