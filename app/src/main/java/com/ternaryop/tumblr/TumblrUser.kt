package com.ternaryop.tumblr

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

/**
 * Created by dave on 18/01/15.
 * Contain tumblr user details
 */
@Suppress("MemberVisibilityCanBePrivate")
class TumblrUser @Throws(JSONException::class) constructor(json: JSONObject) : Serializable {
    var name = json.getString("name")!!
    var isFollowing = json.getBoolean("following")
    var url = json.getString("url")!!
    var updated = json.getLong("updated")

    override fun toString(): String {
        return "$name is following? $isFollowing last update $updated url $url"
    }
}
