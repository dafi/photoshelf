package com.ternaryop.tumblr

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable

@Suppress("MemberVisibilityCanBePrivate")
open class TumblrPost : Serializable {
    var blogName = ""
    var postId: Long = 0
    var postUrl = ""
    var type = ""
    var timestamp: Long = 0
    var date = ""
    var format = ""
    var reblogKey = ""
    var isBookmarklet: Boolean = false
    var isMobile: Boolean = false
    var sourceUrl: String? = null
    var sourceTitle: String? = null
    var isLiked: Boolean = false
    var state = ""
    var totalPosts: Long = 0
    var noteCount: Long = 0

    // queue posts
    var scheduledPublishTime: Long = 0

    var tags: MutableList<String> = mutableListOf()

    val tagsAsString: String
        get() = if (tags.isEmpty()) "" else tags.joinToString(",")

    /**
     * Protect against IndexOutOfBoundsException returning an empty string
     * @return the first tag or an empty string
     */
    val firstTag: String
        get() = if (tags.isEmpty()) "" else tags[0]

    constructor()

    @Throws(JSONException::class)
    constructor(json: JSONObject) {
        blogName = json.getString("blog_name")
        postId = json.getLong("id")
        postUrl = json.getString("post_url")
        type = json.getString("type")
        timestamp = json.getLong("timestamp")
        date = json.getString("date")
        format = json.getString("format")
        reblogKey = json.getString("reblog_key")
        isBookmarklet = json.optBoolean("bookmarklet", false)
        isMobile = json.optBoolean("mobile", false)
        sourceUrl = json.optString("source_url")
        sourceTitle = json.optString("source_title")
        isLiked = json.optBoolean("liked", false)
        state = json.getString("state")
        totalPosts = json.optLong("total_posts", 0)
        noteCount = json.optLong("note_count", 0)

        val jsonTags = json.getJSONArray("tags")
        for (i in 0 until jsonTags.length()) {
            tags.add(jsonTags.getString(i))
        }

        scheduledPublishTime = json.optLong("scheduled_publish_time", 0)
    }

    constructor(post: TumblrPost) {
        blogName = post.blogName
        postId = post.postId
        postUrl = post.postUrl
        type = post.type
        timestamp = post.timestamp
        date = post.date
        format = post.format
        reblogKey = post.reblogKey
        isBookmarklet = post.isBookmarklet
        isMobile = post.isMobile
        sourceUrl = post.sourceUrl
        sourceTitle = post.sourceTitle
        isLiked = post.isLiked
        state = post.state
        totalPosts = post.totalPosts
        noteCount = post.noteCount

        tags = post.tags
        scheduledPublishTime = post.scheduledPublishTime
    }

    fun tagsFromString(str: String) {
        tags = TumblrPost.tagsFromString(str)
    }

    companion object {
        /**
         *
         */
        private const val serialVersionUID = 9136359874716067522L

        fun tagsFromString(str: String) = str.split(",")
            .map { it.trim() }.filter { it.isNotEmpty() }.mapTo(mutableListOf()) { it }
    }
}
