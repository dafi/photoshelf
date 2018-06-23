package com.ternaryop.photoshelf.api.extractor

import org.json.JSONException
import org.json.JSONObject

class ImageInfo @Throws(JSONException::class) constructor(json: JSONObject) {
    /**
     * The thumbnail image url.
     * This is present on the HTML document from which pick images
     * @return thumbnail url
     */
    var thumbnailUrl: String? = null
    /**
     * The destination document containing the image url
     * @return destination document url
     */
    var documentUrl: String? = null
    /**
     * The image url present inside the destination document url.
     * If null must be retrieved from destination document
     * @return image url
     */
    var imageUrl: String? = null

    override fun toString(): String {
        return "thumb $thumbnailUrl doc $documentUrl"
    }

    init {
        thumbnailUrl = json.getString("thumbnailUrl")
        documentUrl = json.optString("documentUrl", null)
        imageUrl = json.optString("imageUrl", null)
    }
}
