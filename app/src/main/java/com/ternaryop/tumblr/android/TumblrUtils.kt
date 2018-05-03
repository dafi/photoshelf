package com.ternaryop.tumblr.android

import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPost

fun Tumblr.editTags(postId: Long, blogName: String, tagsToAdd: List<String>): List<String>? {
    val postsList = getPublicPosts(blogName, mapOf("id" to postId.toString()))

    if (postsList.isEmpty()) {
        return null
    }

    val tagList = TumblrPost.tagsFromString(postsList[0].tagsAsString)
    val currentCount = tagList.size
    for (t in tagsToAdd) {
        if (!tagList.contains(t)) {
            tagList.add(t)
        }
    }
    // nothing added
    if (currentCount == tagList.size) {
        return null
    }

    val params = mapOf(
    "id" to postId.toString(),
    "tags" to tagList.joinToString(",")
    )
    editPost(blogName, params)
    return tagList
}

fun Tumblr.renameTag(fromTag: String, toTag: String, blogName: String, onEdited: (post: TumblrPost) -> Unit): Int {
    val searchParams = HashMap<String, String>()
    searchParams["type"] = "photo"
    searchParams["tag"] = fromTag
    var offset = 0
    var loadNext: Boolean
    val params = HashMap<String, String>()
    var renamedCount = 0

    do {
        searchParams["offset"] = offset.toString()
        val postsList = getPublicPosts(blogName, searchParams)
        loadNext = postsList.isNotEmpty()
        offset += postsList.size

        for (post in postsList) {
            if (replaceTag(fromTag, toTag, post)) {
                params.clear()
                params["id"] = post.postId.toString()
                params["tags"] = post.tagsAsString
                editPost(blogName, params)
                onEdited(post)
//                DBHelper.getInstance(context).postDAO.updateTags(context, post.postId, post.tagsAsString, blogName)
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
