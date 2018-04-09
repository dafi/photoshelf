package com.ternaryop.tumblr

import com.ternaryop.tumblr.Tumblr.Companion.getApiUrl
import org.json.JSONException
import java.io.File
import java.net.URI

/**
 * Created by dave on 12/03/18.
 * Tumblr photo API
 */
fun Tumblr.draftPhotoPost(tumblrName: String, uri: URI, caption: String, tags: String) {
    try {
        createPhotoPost(tumblrName, uri, caption, tags, "draft")
    } catch (e: JSONException) {
        throw TumblrException(e)
    }
}

fun Tumblr.publishPhotoPost(tumblrName: String, uri: URI, caption: String, tags: String) {
    try {
        createPhotoPost(tumblrName, uri, caption, tags, "published")
    } catch (e: JSONException) {
        throw TumblrException(e)
    }
}

private fun Tumblr.createPhotoPost(tumblrName: String, uri: URI, caption: String, tags: String, state: String): Long {
    val apiUrl = getApiUrl(tumblrName, "/post")
    val params = HashMap<String, Any>()

    if (uri.scheme == "file") {
        params["data"] = File(uri.path)
    } else {
        params["source"] = uri.toString()
    }
    params["state"] = state
    params["type"] = "photo"
    params["caption"] = caption
    params["tags"] = tags

    val json = consumer.jsonFromPost(apiUrl, params)
    return json.getJSONObject("response").getLong("id")
}

fun Tumblr.getPhotoPosts(tumblrName: String, params: Map<String, String>): List<TumblrPhotoPost> {
    val apiUrl = getApiUrl(tumblrName, "/posts/photo")
    val list = mutableListOf<TumblrPhotoPost>()

    try {
        val paramsWithKey = HashMap(params)
        paramsWithKey["api_key"] = consumer.consumerKey

        val json = consumer.jsonFromGet(apiUrl, paramsWithKey)
        val arr = json.getJSONObject("response").getJSONArray("posts")
        val totalPosts = json.getJSONObject("response").optLong("total_posts", -1)
        for (i in 0 until arr.length()) {
            val post = Tumblr.build(arr.getJSONObject(i)) as TumblrPhotoPost
            if (totalPosts != -1L) {
                post.totalPosts = totalPosts
            }
            list.add(post)
        }
    } catch (e: Exception) {
        throw TumblrException(e)
    }

    return list
}


