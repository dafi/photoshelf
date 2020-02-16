package com.ternaryop.photoshelf.activity.impl

import android.content.Context
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.imagepicker.activity.ImagePickerActivity
import com.ternaryop.photoshelf.imageviewer.activity.ImageViewerActivity
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.ImageViewerData
import com.ternaryop.photoshelf.tagphotobrowser.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData

class ImageViewerActivityStarterImpl : ImageViewerActivityStarter {
    override fun startImagePicker(context: Context, url: String) = ImagePickerActivity.startImagePicker(context, url)

    override fun startTagPhotoBrowser(
        context: Context,
        data: TagPhotoBrowserData
    ) = TagPhotoBrowserActivity.startPhotoBrowserActivity(context, data)

    override fun startTagPhotoBrowserForResult(
        fragment: Fragment,
        requestCode: Int,
        data: TagPhotoBrowserData
    ) = TagPhotoBrowserActivity.startPhotoBrowserActivityForResult(
        fragment,
        requestCode,
        data)

    override fun startImageViewer(
        context: Context,
        data: ImageViewerData
    ) = ImageViewerActivity.startImageViewer(context, data)
}
