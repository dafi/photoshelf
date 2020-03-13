package com.ternaryop.photoshelf.activity.impl

import android.content.Context
import androidx.fragment.app.Fragment
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.activity.ImageViewerData
import com.ternaryop.photoshelf.activity.TagPhotoBrowserData
import com.ternaryop.photoshelf.imagepicker.activity.ImagePickerActivity
import com.ternaryop.photoshelf.imagepicker.repository.ImageGalleryRepository
import com.ternaryop.photoshelf.imageviewer.activity.ImageViewerActivity
import com.ternaryop.photoshelf.tagphotobrowser.activity.TagPhotoBrowserActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class ImageViewerActivityStarterImpl(
    private val imageGalleryRepository: ImageGalleryRepository
) : ImageViewerActivityStarter, CoroutineScope {
    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    override fun startImagePicker(context: Context, url: String) = ImagePickerActivity.startImagePicker(context, url)

    override fun startImagePickerPrefetch(urls: List<String>) {
        launch {
            imageGalleryRepository.prefetch(urls)
        }
    }

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
