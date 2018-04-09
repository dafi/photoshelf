package com.ternaryop.tumblr.android

import android.content.Context
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.tumblr.TumblrPost

object TumblrUtils {
    fun renameTag(fromTag: String, toTag: String, context: Context, blogName: String): Int {
        val searchParams = HashMap<String, String>()
        searchParams["type"] = "photo"
        searchParams["tag"] = fromTag
        var offset = 0
        var loadNext: Boolean
        val tumblr = TumblrManager.getInstance(context)
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
