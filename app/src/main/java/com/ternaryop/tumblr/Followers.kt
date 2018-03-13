package com.ternaryop.tumblr

import org.json.JSONException

/**
 * Created by dave on 12/03/18.
 * Tumblr followers API
 */
fun Tumblr.getFollowers(
    tumblrName: String,
    params: Map<String, String>? = null,
    followers: TumblrFollowers? = null): TumblrFollowers {
    val apiUrl = Tumblr.getApiUrl(tumblrName, "/followers")

    val modifiedParams = HashMap<String, String>()
    if (params != null) {
        modifiedParams.putAll(params)
    }
    modifiedParams["base-hostname"] = "$tumblrName.tumblr.com"

    try {
        val json = consumer.jsonFromGet(apiUrl, modifiedParams)
        val resultFollowers = followers ?: TumblrFollowers()
        resultFollowers.add(json.getJSONObject("response"))

        return resultFollowers
    } catch (e: JSONException) {
        throw TumblrException(e)
    }
}

fun Tumblr.getFollowers(tumblrName: String, offset: Int, limit: Int, followers: TumblrFollowers): TumblrFollowers {
    val params = HashMap<String, String>()
    params["offset"] = Integer.toString(offset)
    params["limit"] = Integer.toString(limit)

    return getFollowers(tumblrName, params, followers)
}

