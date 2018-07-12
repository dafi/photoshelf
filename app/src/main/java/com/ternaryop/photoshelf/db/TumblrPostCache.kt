package com.ternaryop.photoshelf.db

import com.ternaryop.tumblr.TumblrPost
import java.io.Serializable

class TumblrPostCache(var id: String, post: TumblrPost, var cacheType: Int) : Serializable {
    var blogName = post.blogName
    var timestamp = post.timestamp
    var post: TumblrPost? = post

    companion object {
        const val CACHE_TYPE_DRAFT = 0
    }
}