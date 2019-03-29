package com.ternaryop.tumblr

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

class TumblrPhoto @Throws(JSONException::class) constructor(json: JSONObject) : Serializable {
    var caption: String? = json.getString("caption")
    val altSizes = mutableListOf<TumblrAltSize>()

    companion object {
        /**
         *
         */
        private const val serialVersionUID = -5458693563884708164L
    }

    init {
        val jsonSizes = json.getJSONArray("alt_sizes")
        (0 until jsonSizes.length()).mapTo(altSizes) { TumblrAltSize(jsonSizes.getJSONObject(it)) }
    }
}
