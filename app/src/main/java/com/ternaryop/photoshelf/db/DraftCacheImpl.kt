package com.ternaryop.photoshelf.db

import com.ternaryop.photoshelf.tumblr.ui.draft.DraftCache
import com.ternaryop.tumblr.TumblrPost

private const val CACHE_TYPE = TumblrPostCache.CACHE_TYPE_DRAFT

class DraftCacheImpl(val dao: TumblrPostCacheDAO) : DraftCache {
    override fun read(blogName: String): List<TumblrPost> = dao.read(blogName, CACHE_TYPE)

    override fun clear(blogName: String?) = dao.clearCache(CACHE_TYPE)

    override fun update(post: TumblrPost): Boolean = dao.updateItem(post, CACHE_TYPE)

    override fun insert(post: TumblrPost): Long = dao.insertItem(post, CACHE_TYPE)

    override fun delete(post: TumblrPost): Int = dao.deleteItem(post)

    override fun deleteById(postIds: Collection<String>, blogName: String) = dao.delete(postIds, CACHE_TYPE, blogName)

    override fun delete(posts: List<TumblrPost>) = dao.delete(posts, CACHE_TYPE)

    override fun insert(posts: Collection<TumblrPost>) = dao.write(posts, CACHE_TYPE)

    override fun findMostRecentTimestamp(blogName: String): Long = dao.findMostRecentTimestamp(blogName, CACHE_TYPE)
}
