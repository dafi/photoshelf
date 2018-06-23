package com.ternaryop.photoshelf.api.extractor

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by dave on 01/04/17.
 * The mapping object used to hold the Gallery result
 */

class ImageGallery @Throws(JSONException::class) constructor(json: JSONObject) {
    var domain: String? = null
    var title: String? = null
    val imageInfoList: Array<ImageInfo>

    init {
        domain = json.getString("domain")
        title = json.getString("title")
        val array = json.getJSONArray("gallery")
        imageInfoList = Array(array.length()) { ImageInfo(array.getJSONObject(it)) }
    }
}
