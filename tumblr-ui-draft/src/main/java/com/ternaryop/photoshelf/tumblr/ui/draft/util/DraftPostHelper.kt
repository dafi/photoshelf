package com.ternaryop.photoshelf.tumblr.ui.draft.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.LatestTimestampResult
import com.ternaryop.photoshelf.tumblr.ui.draft.DraftCache
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getDraftPosts
import com.ternaryop.tumblr.queueAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private const val PREF_DRAFT_LAST_TIMESTAMP = "draft_last_timeStamp"

class DraftPostHelper(context: Context, private val draftCache: DraftCache) {
    var blogName = ""
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val tumblr = TumblrManager.getInstance(context.applicationContext)

    private suspend fun getQueuePosts(): List<TumblrPost> = coroutineScope {
        tumblr.queueAll(blogName)
    }

    private suspend fun getNewerDraftPosts(maxTimestamp: Long): List<TumblrPost> = coroutineScope {
        tumblr.getDraftPosts(blogName, maxTimestamp)
    }

    suspend fun getLastPublishedTimestamp(): LatestTimestampResult {
        val lastTimestamp = preferences.getLong(PREF_DRAFT_LAST_TIMESTAMP, -1)
        return ApiManager
            .postService()
            .getLastPublishedTimestamp(blogName, lastTimestamp)
            .response
    }

    fun refreshCache(last: LatestTimestampResult) {
        preferences.edit().putLong(PREF_DRAFT_LAST_TIMESTAMP, last.lastPublishTimestamp).apply()
        // delete from cache the published posts
        last.publishedIdList?.let { draftCache.deleteById(it, blogName) }
    }

    private fun purgeDraftCached(
        blogName: String,
        newerDraftPosts: List<TumblrPost>,
        queuePosts: List<TumblrPost>
    ): List<TumblrPost> {
        // update the cache with new draft posts
        draftCache.insert(newerDraftPosts)
        // delete from the cache any post moved from draft to scheduled
        draftCache.delete(queuePosts)
        // return the updated/cleaned up cache
        return draftCache.read(blogName)
    }

    suspend fun getDraftQueuePosts() = coroutineScope {
        val maxTimestamp = draftCache.findMostRecentTimestamp(blogName)
        val newerDraftPostsDeferred = async(Dispatchers.IO) { getNewerDraftPosts(maxTimestamp) }
        val queuedPostsDeferred = async(Dispatchers.IO) { getQueuePosts() }
        val newerDraftPosts = newerDraftPostsDeferred.await()
        val queuedPosts = queuedPostsDeferred.await()
        DraftQueuePosts(purgeDraftCached(blogName, newerDraftPosts, queuedPosts).toMutableList(), queuedPosts)
    }
}
