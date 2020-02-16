package com.ternaryop.photoshelf.imagepicker.fragment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.extractor.ImageGalleryResult
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.domselector.DomSelectorManager
import com.ternaryop.photoshelf.domselector.util.readImageGallery
import com.ternaryop.photoshelf.domselector.util.retrieveImageUri
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.lifecycle.ProgressData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

typealias ImageInfoUriPair = Pair<ImageInfo, Uri>

class ImagePickerViewModel(application: Application) : PhotoShelfViewModel<ImagePickerModelResult>(application) {
    private val domSelectors = DomSelectorManager.selectors(application)
    private var galleryResult: ImageGalleryResult? = null

    fun readImageGallery(url: String) {
        if (galleryResult?.gallery?.url == url) {
            postResult(ImagePickerModelResult.Gallery(Command.success(galleryResult)))
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute { domSelectors.readImageGallery(url).response }
            postResult(ImagePickerModelResult.Gallery(command))
        }
    }

    fun imageList(imageInfoList: List<ImageInfo>, destDirectory: File? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                val uriList = mutableListOf<ImageInfoUriPair>()

                imageInfoList.forEachIndexed { index, imageInfo ->
                    domSelectors.retrieveImageUri(imageInfo, destDirectory)?.let { uriList.add(Pair(imageInfo, it)) }
                    postResult(ImagePickerModelResult.ImageList(
                        Command.progress(ProgressData(index, imageInfoList.size))))
                }
                uriList
            }
            postResult(ImagePickerModelResult.ImageList(command))
        }
    }

    fun image(imageInfo: ImageInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                domSelectors.retrieveImageUri(imageInfo)?.let { Pair(imageInfo, it) }
                    ?: throw IOException("Unable to find Url for ${imageInfo.documentUrl}")
            }
            postResult(ImagePickerModelResult.Image(command))
        }
    }
}

sealed class ImagePickerModelResult {
    data class Gallery(val command: Command<ImageGalleryResult>) : ImagePickerModelResult()
    data class ImageList(val command: Command<List<ImageInfoUriPair>>) : ImagePickerModelResult()
    data class Image(val command: Command<ImageInfoUriPair>) : ImagePickerModelResult()
}
