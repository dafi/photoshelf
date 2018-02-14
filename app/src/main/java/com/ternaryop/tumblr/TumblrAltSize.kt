package com.ternaryop.tumblr

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

class TumblrAltSize @Throws(JSONException::class) constructor(json: JSONObject) : Serializable {
    var width: Int = json.getInt("width")
    var height: Int = json.getInt("height")
    var url: String = json.getString("url")

    override fun toString() = "${width}x$height"

    companion object {
        private const val serialVersionUID = 7556335251472323825L
    }
}
