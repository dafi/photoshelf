package com.ternaryop.tumblr

import org.json.JSONArray

fun Tumblr.getDraftPosts(tumblrName: String, maxTimestamp: Long): List<TumblrPost> {
    val apiUrl = Tumblr.getApiUrl(tumblrName, "/posts/draft")
    val list = mutableListOf<TumblrPost>()

    try {
        val json = consumer.jsonFromGet(apiUrl)
        var arr = json.getJSONObject("response").getJSONArray("posts")

        val params = HashMap<String, String>(1)
        while (arr.length() > 0 && addNewerPosts(list, arr, maxTimestamp)) {
            val beforeId = arr.getJSONObject(arr.length() - 1).getLong("id")
            params["before_id"] = beforeId.toString() + ""

            arr = consumer.jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts")
        }
    } catch (e: Exception) {
        throw TumblrException(e)
    }

    return list
}

fun Tumblr.saveDraft(tumblrName: String, id: Long) {
    val apiUrl = Tumblr.getApiUrl(tumblrName, "/post/edit")

    val params = HashMap<String, String>()
    params["id"] = id.toString()
    params["state"] = "draft"

    try {
        consumer.jsonFromPost(apiUrl, params)
    } catch (e: Exception) {
        throw TumblrException(e)
    }
}

/**
 * Add to list the posts with timestamp greater than maxTimestamp (ie newer posts)
 * @return true if all posts in jsonPosts are newer, false otherwise
 */
private fun addNewerPosts(list: MutableList<TumblrPost>, jsonPosts: JSONArray, maxTimestamp: Long): Boolean {
    for (i in 0 until jsonPosts.length()) {
        val post = Tumblr.build(jsonPosts.getJSONObject(i))
        if (post.timestamp <= maxTimestamp) {
            return false
        }
        list.add(post)
    }
    return true
}

fun Tumblr.draftCount(tumblrName: String): Int {
    val apiUrl = Tumblr.getApiUrl(tumblrName, "/posts/draft")
    var count = 0

    try {
        val json = consumer.jsonFromGet(apiUrl)
        var arr = json.getJSONObject("response").getJSONArray("posts")

        val params = HashMap<String, String>(1)
        while (arr.length() > 0) {
            count += arr.length()
            params["before_id"] = arr.getJSONObject(arr.length() - 1).getString("id")

            arr = consumer.jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts")
        }
    } catch (e: Exception) {
        throw TumblrException(e)
    }

    return count
}
