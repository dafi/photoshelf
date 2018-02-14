package com.ternaryop.photoshelf.db

import com.ternaryop.tumblr.TumblrPost
import java.util.Locale

class PostTag : Post {
    var tag: String = ""
    set(value) {
        field = value.toLowerCase(Locale.US)
    }
    var tumblrName: String? = null

    constructor(id: Long, tumblrName: String, tag: String, timestamp: Long, showOrder: Int) :
            super(id, 0, 0, timestamp, showOrder) {
        this.tumblrName = tumblrName
        this.tag = tag
    }

    constructor(post: Post) : super(post.id, 0, 0, post.publishTimestamp, post.showOrder)

    override fun toString(): String = "$tumblrName[$id] = tag $tag ts = $publishTimestamp order $showOrder"

    companion object {
        private const val serialVersionUID = 5674124483160664227L

        fun from(input: Collection<TumblrPost>): List<PostTag> {
            val list = mutableListOf<PostTag>()

            for (p in input) {
                list.addAll(from(p))
            }

            return list
        }

        fun from(post: TumblrPost): List<PostTag> {
            return post.tags.mapIndexed { index, it -> PostTag(post.postId, post.blogName, it, post.timestamp, index + 1) }
        }
    }
}