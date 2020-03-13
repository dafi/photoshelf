package com.ternaryop.photoshelf.imagepicker.fragment

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.extractor.ImageGalleryResult
import com.ternaryop.photoshelf.api.extractor.ImageInfo
import com.ternaryop.photoshelf.imagepicker.repository.ImageGalleryRepository
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.lifecycle.ProgressData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

typealias ImageInfoUriPair = Pair<ImageInfo, Uri>

class ImagePickerViewModel(
    application: Application,
    private val imageGalleryRepository: ImageGalleryRepository
) : PhotoShelfViewModel<ImagePickerModelResult>(application) {

    fun readImageGallery(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute { imageGalleryRepository.readImageGallery(url) }
            postResult(ImagePickerModelResult.Gallery(command))
        }
    }

    fun imageList(imageInfoList: List<ImageInfo>, destDirectory: File? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                val uriList = mutableListOf<ImageInfoUriPair>()

                imageInfoList.forEachIndexed { index, imageInfo ->
                    imageGalleryRepository.retrieveImageUri(imageInfo, destDirectory)?.let {
                        uriList.add(Pair(imageInfo, it))
                    }
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
            val command = Command.execute { imageGalleryRepository.image(imageInfo) }
            postResult(ImagePickerModelResult.Image(command))
        }
    }
}

sealed class ImagePickerModelResult {
    data class Gallery(val command: Command<ImageGalleryResult>) : ImagePickerModelResult()
    data class ImageList(val command: Command<List<ImageInfoUriPair>>) : ImagePickerModelResult()
    data class Image(val command: Command<ImageInfoUriPair>) : ImagePickerModelResult()
}
