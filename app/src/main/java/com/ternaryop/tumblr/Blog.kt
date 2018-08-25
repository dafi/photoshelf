package com.ternaryop.tumblr

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

class Blog @Throws(JSONException::class) constructor(jsonResponse: JSONObject) : Serializable {

    val name: String = jsonResponse.getString("name")
    val url: String = jsonResponse.getString("url")
    val title: String = jsonResponse.getString("title")
    val isPrimary = jsonResponse.getBoolean("primary")

    fun getAvatarUrlBySize(size: Int): String = getAvatarUrlBySize(name, size)

    override fun toString(): String = name

    companion object {
        private const val serialVersionUID = -7241228948040188270L

        fun getAvatarUrlBySize(baseHost: String, size: Int): String {
            return "https://api.tumblr.com/v2/blog/$baseHost.tumblr.com/avatar/$size"
        }
    }
}
