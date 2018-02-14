package com.ternaryop.tumblr

import android.content.Context
import com.ternaryop.photoshelf.db.DBHelper

object TumblrUtils {
    fun getQueueCount(tumblr: Tumblr, tumblrName: String): Long {
        // do not use Tumblr.getQueue() because it creates unused TumblrPost
        val apiUrl = tumblr.getApiUrl(tumblrName, "/posts/queue")
        var count: Long = 0
        var readCount: Long

        try {
            val params = HashMap<String, String>(1)
            do {
                val arr = tumblr.consumer.jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts")
                readCount = arr.length().toLong()
                count += readCount
                params["offset"] = count.toString()
            } while (readCount == 20L)
        } catch (e: Exception) {
            throw TumblrException(e)
        }

        return count
    }

    fun getDraftCount(tumblr: Tumblr, tumblrName: String): Long {
        val apiUrl = tumblr.getApiUrl(tumblrName, "/posts/draft")
        var count: Long = 0

        try {
            val json = tumblr.consumer.jsonFromGet(apiUrl)
            var arr = json.getJSONObject("response").getJSONArray("posts")

            val params = HashMap<String, String>(1)
            while (arr.length() > 0) {
                count += arr.length().toLong()
                val beforeId = arr.getJSONObject(arr.length() - 1).getLong("id")
                params["before_id"] = beforeId.toString() + ""

                arr = tumblr.consumer.jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts")
            }
        } catch (e: Exception) {
            throw TumblrException(e)
        }

        return count
    }

    fun getQueueAll(tumblr: Tumblr, tumblrName: String): List<TumblrPost> {
        val list = mutableListOf<TumblrPost>()
        var readCount: Long

        try {
            val params = HashMap<String, String>(1)
            do {
                val queue = tumblr.getQueue(tumblrName, params)
                readCount = queue.size.toLong()
                list.addAll(queue)
                params["offset"] = list.size.toString()
            } while (readCount == 20L)
        } catch (e: Exception) {
            throw TumblrException(e)
        }

        return list
    }

    fun renameTag(fromTag: String, toTag: String, context: Context, blogName: String): Int {
        val searchParams = HashMap<String, String>()
        searchParams["type"] = "photo"
        searchParams["tag"] = fromTag
        var offset = 0
        var loadNext: Boolean
        val tumblr = Tumblr.getSharedTumblr(context)
        val params = HashMap<String, String>()
        var renamedCount = 0

        do {
            searchParams["offset"] = offset.toString()
            val postsList = tumblr.getPublicPosts(blogName, searchParams)
            loadNext = postsList.isNotEmpty()
            offset += postsList.size

            for (post in postsList) {
                if (replaceTag(fromTag, toTag, post)) {
                    params.clear()
                    params["id"] = post.postId.toString()
                    params["tags"] = post.tagsAsString
                    tumblr.editPost(blogName, params)
                    updateTagsOnDB(context, post.postId, post.tagsAsString, blogName)
                    ++renamedCount
                }
            }
        } while (loadNext)
        return renamedCount
    }

    private fun replaceTag(fromTag: String, toTag: String, post: TumblrPost): Boolean {
        val renamedTag = ArrayList(post.tags)
        var found = false
        for (i in renamedTag.indices) {
            if (renamedTag[i].equals(fromTag, ignoreCase = true)) {
                renamedTag.removeAt(i)
                renamedTag.add(i, toTag)
                found = true
                break
            }
        }
        if (found) {
            post.tags = renamedTag
        }
        return found
    }

    private fun updateTagsOnDB(context: Context, id: Long, tags: String, blogName: String) {
        val newValues = HashMap<String, String>()
        newValues["id"] = id.toString()
        newValues["tags"] = tags
        newValues["tumblrName"] = blogName
        DBHelper.getInstance(context).postDAO.update(context, newValues)
    }
}
