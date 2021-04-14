package com.ternaryop.photoshelf.activity.impl

import android.content.Context
import android.content.Intent
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class ImageViewerActivityStarterImpl @Inject constructor(
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

    override fun tagPhotoBrowserIntent(
        context: Context,
        tagPhotoBrowserData: TagPhotoBrowserData,
        returnSelectedPost: Boolean
    ): Intent = TagPhotoBrowserActivity.createIntent(context, tagPhotoBrowserData, returnSelectedPost)

    override fun startImageViewer(
        context: Context,
        data: ImageViewerData
    ) = ImageViewerActivity.startImageViewer(context, data)
}
