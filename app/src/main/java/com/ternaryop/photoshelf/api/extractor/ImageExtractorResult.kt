package com.ternaryop.photoshelf.api.extractor

import com.google.gson.annotations.SerializedName

/**
 * Created by dave on 01/04/17.
 * The mapping object used to hold the Gallery result
 */

data class ImageGalleryResult(val gallery: ImageGallery)
class ImageGallery(val domain: String? = null,
    val title: String? = null,
    @SerializedName("gallery") val imageInfoList: Array<ImageInfo>)

data class ImageResult(val imageUrl: String)

/**
 * @param thumbnailUrl The thumbnail image url. This is present on the HTML document from which pick images
 * @param documentUrl The destination document containing the image url
 * @param imageUrl The image url present inside the destination document url. If null must be retrieved from
 * destination document
 */
class ImageInfo(val thumbnailUrl: String? = null, var documentUrl: String? = null, var imageUrl: String? = null)
