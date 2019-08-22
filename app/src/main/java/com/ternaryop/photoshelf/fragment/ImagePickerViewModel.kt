package com.ternaryop.photoshelf.fragment

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

    fun readImageGallery(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                postResult(ImagePickerModelResult.Gallery(Command.success(domSelectors.readImageGallery(url).response)))
            } catch (t: Throwable) {
                postResult(ImagePickerModelResult.Gallery(Command.error(t)))
            }
        }
    }

    fun imageList(imageInfoList: List<ImageInfo>, destDirectory: File? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uriList = mutableListOf<ImageInfoUriPair>()

                imageInfoList.forEachIndexed { index, imageInfo ->
                    domSelectors.retrieveImageUri(imageInfo, destDirectory)?.let { uriList.add(Pair(imageInfo, it)) }
                    postResult(ImagePickerModelResult.ImageList(Command.progress(ProgressData(index, imageInfoList.size))))
                }

                postResult(ImagePickerModelResult.ImageList(Command.success(uriList)))
            } catch (t: Throwable) {
                postResult(ImagePickerModelResult.ImageList(Command.error(t)))
            }
        }
    }

    fun image(imageInfo: ImageInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = domSelectors.retrieveImageUri(imageInfo)?.let { Pair(imageInfo, it) }
                    ?: throw IOException("Unable to find Url for ${imageInfo.documentUrl}")

                postResult(ImagePickerModelResult.Image(Command.success(uri)))
            } catch (t: Throwable) {
                postResult(ImagePickerModelResult.Image(Command.error(t)))
            }
        }
    }
}

sealed class ImagePickerModelResult {
    data class Gallery(val command: Command<ImageGalleryResult>) : ImagePickerModelResult()
    data class ImageList(val command: Command<List<ImageInfoUriPair>>) : ImagePickerModelResult()
    data class Image(val command: Command<ImageInfoUriPair>) : ImagePickerModelResult()
}
