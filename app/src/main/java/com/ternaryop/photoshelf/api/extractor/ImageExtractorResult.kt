package com.ternaryop.photoshelf.api.extractor

import com.google.gson.annotations.SerializedName
import com.ternaryop.photoshelf.api.parser.TitleComponentsResult

/**
 * Created by dave on 01/04/17.
 * The mapping object used to hold the Gallery result
 */

data class ImageGalleryResult(val gallery: ImageGallery)
class ImageGallery(val domain: String? = null,
    val title: String? = null,
    var titleParsed: TitleComponentsResult,
    @SerializedName("gallery") val imageInfoList: List<ImageInfo>) {
    /**
     * Return a string that can be used by the title parser
     * @return the title plus domain string
     */
    val parsableTitle: String
        get() = "$title ::::: $domain"
}

data class ImageResult(val imageUrl: String)

/**
 * @param thumbnailUrl The thumbnail image url. This is present on the HTML document from which pick images
 * @param documentUrl The destination document containing the image url
 * @param imageUrl The image url present inside the destination document url. If null must be retrieved from
 * destination document
 */
class ImageInfo(val thumbnailUrl: String? = null, var documentUrl: String? = null, var imageUrl: String? = null)
