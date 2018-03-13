package com.ternaryop.tumblr

import com.ternaryop.tumblr.Tumblr.Companion.getApiUrl
import org.json.JSONException

/**
 * Created by dave on 12/03/18.
 * Tumblr text API
 */
fun Tumblr.draftTextPost(tumblrName: String, title: String, body: String, tags: String) {
    try {
        createTextPost(tumblrName, title, body, tags, "draft")
    } catch (e: JSONException) {
        throw TumblrException(e)
    }
}

fun Tumblr.publishTextPost(tumblrName: String, title: String, body: String, tags: String) {
    try {
        createTextPost(tumblrName, title, body, tags, "published")
    } catch (e: JSONException) {
        throw TumblrException(e)
    }
}

private fun Tumblr.createTextPost(tumblrName: String, title: String, body: String, tags: String, state: String): Long {
    val apiUrl = getApiUrl(tumblrName, "/post")
    val params = HashMap<String, Any>()

    params["state"] = state
    params["type"] = "text"
    params["title"] = title
    params["body"] = body
    params["tags"] = tags

    val json = consumer.jsonFromPost(apiUrl, params)
    return json.getJSONObject("response").getLong("id")
}

