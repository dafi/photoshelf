package com.ternaryop.photoshelf.tumblr.ui.draft

import com.ternaryop.tumblr.TumblrPost

interface DraftCache {
    fun read(blogName: String): List<TumblrPost>
    /**
     * clear the entire cache, if [blogName] is not null just clear its cache
     */
    fun clear(blogName: String? = null)
    fun update(post: TumblrPost): Boolean
    fun insert(post: TumblrPost): Long
    fun insert(posts: Collection<TumblrPost>)
    fun delete(post: TumblrPost): Int
    fun delete(posts: List<TumblrPost>)
    fun deleteById(postIds: Collection<String>, blogName: String)
    fun findMostRecentTimestamp(blogName: String): Long
}
