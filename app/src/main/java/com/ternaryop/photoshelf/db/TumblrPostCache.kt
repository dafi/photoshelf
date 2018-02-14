package com.ternaryop.photoshelf.db

import com.ternaryop.tumblr.TumblrPost
import java.io.Serializable

class TumblrPostCache : Serializable {

    lateinit var id: String
    lateinit var blogName: String
    var cacheType: Int = 0
    var timestamp: Long = 0
    var post: TumblrPost? = null

    constructor()

    constructor(id: String, post: TumblrPost, cacheType: Int) {
        this.id = id
        this.blogName = post.blogName
        this.cacheType = cacheType
        this.timestamp = post.timestamp
        this.post = post
    }

    companion object {
        const val CACHE_TYPE_DRAFT = 0
    }
}