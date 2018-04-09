package com.ternaryop.tumblr

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Tumblr.getQueue(tumblrName: String, params: Map<String, String>): List<TumblrPost> {
    val apiUrl = Tumblr.getApiUrl(tumblrName, "/posts/queue")
    val list = mutableListOf<TumblrPost>()

    try {
        val json = consumer.jsonFromGet(apiUrl, params)
        val arr = json.getJSONObject("response").getJSONArray("posts")
        Tumblr.addPostsToList(list, arr)
    } catch (e: Exception) {
        throw TumblrException(e)
    }

    return list
}

fun Tumblr.schedulePost(tumblrName: String, post: TumblrPost, timestamp: Long): Long {
    try {
        val apiUrl = Tumblr.getApiUrl(tumblrName, "/post/edit")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
        val gmtDate = dateFormat.format(Date(timestamp))

        val params = HashMap<String, String>()
        params["id"] = post.postId.toString() + ""
        params["state"] = "queue"
        params["publish_on"] = gmtDate

        if (post is TumblrPhotoPost) {
            params["caption"] = post.caption
        }
        params["tags"] = post.tagsAsString

        return consumer.jsonFromPost(apiUrl, params).getJSONObject("response").getLong("id")
    } catch (e: Exception) {
        throw TumblrException(e)
    }
}

fun Tumblr.queueCount(tumblrName: String): Int {
    // do not use Tumblr.getQueue() because it creates unused TumblrPost
    val apiUrl = Tumblr.getApiUrl(tumblrName, "/posts/queue")
    var count = 0
    var readCount: Int

    try {
        val params = HashMap<String, String>(1)
        do {
            val arr = consumer.jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts")
            readCount = arr.length()
            count += readCount
            params["offset"] = count.toString()
        } while (readCount == Tumblr.MAX_POST_PER_REQUEST)
    } catch (e: Exception) {
        throw TumblrException(e)
    }

    return count
}

fun Tumblr.queueAll(tumblrName: String): List<TumblrPost> {
    val list = mutableListOf<TumblrPost>()
    var readCount: Int

    try {
        val params = HashMap<String, String>(1)
        do {
            val queue = getQueue(tumblrName, params)
            readCount = queue.size
            list.addAll(queue)
            params["offset"] = list.size.toString()
        } while (readCount == Tumblr.MAX_POST_PER_REQUEST)
    } catch (e: Exception) {
        throw TumblrException(e)
    }

    return list
}
