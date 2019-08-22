package com.ternaryop.photoshelf

import android.content.Context
import android.text.format.DateUtils.SECOND_IN_MILLIS
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.LatestTimestampResult
import com.ternaryop.photoshelf.api.post.titlesRequestBody
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.TumblrPostCache
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getDraftPosts
import com.ternaryop.tumblr.queueAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Locale

data class DraftQueuePosts<out T: TumblrPost>(val newerDraftPosts: List<T>, val queuePosts: List<TumblrPost>)

private const val PREF_DRAFT_LAST_TIMESTAMP = "draft_last_timeStamp"

class DraftPostHelper(context: Context) {
    var blogName = ""
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    private val draftCache = DBHelper.getInstance(context.applicationContext).tumblrPostCacheDAO
    private val tumblr = TumblrManager.getInstance(context.applicationContext)

    private suspend fun getQueuePosts(): List<TumblrPost> = coroutineScope {
         tumblr.queueAll(blogName)
    }

    /**
     * Return the map where the key is the first tag and value contains the posts for that tag
     * @param posts posts to group by tag
     * @return the (tag, posts) map
     */
    private fun groupPostByTag(posts: List<TumblrPost>): Map<String, List<TumblrPost>> {
        return posts
            .filter { it.tags.isNotEmpty() && it.type == "photo" }
            .groupBy { it.tags[0].toLowerCase(Locale.US) }
    }

    private suspend fun getTagLastPublishedMap(tags: Set<String>): Map<String, Long> {
        return ApiManager.postService()
            .getMapLastPublishedTimestampTag(blogName, titlesRequestBody(tags))
            .response.pairs
    }

    suspend fun getPhotoShelfPosts(
        draftPosts: List<TumblrPost>,
        queuedPosts: List<TumblrPost>): List<PhotoShelfPost> {
        val tagsForDraftPosts = groupPostByTag(draftPosts)
        val tagsForQueuePosts = groupPostByTag(queuedPosts)
        val lastPublished = getTagLastPublishedMap(tagsForDraftPosts.keys)
        val list = mutableListOf<PhotoShelfPost>()

        for ((tag, posts) in tagsForDraftPosts) {
            val lastPublishedTimestamp = getLastPublishedTimestampByTag(tag, lastPublished)
            val queuedTimestamp = getNextScheduledPublishTimeByTag(tag, tagsForQueuePosts)
            val timestampToSave = if (queuedTimestamp > 0) queuedTimestamp else lastPublishedTimestamp
            for (post in posts) {
                // preserve schedule time when present
                post.scheduledPublishTime = queuedTimestamp / SECOND_IN_MILLIS
                list += PhotoShelfPost(post as TumblrPhotoPost, timestampToSave)
            }
        }
        return list
    }

    private fun getNextScheduledPublishTimeByTag(tag: String, queuedPosts: Map<String, List<TumblrPost>>): Long {
        val list = queuedPosts[tag] ?: return 0

        // posts are sorted by schedule date so it's sufficient to get the first item
        return list[0].scheduledPublishTime * SECOND_IN_MILLIS
    }

    private fun getLastPublishedTimestampByTag(tag: String, lastPublished: Map<String, Long>): Long {
        val lastPublishedTimestamp = lastPublished[tag] ?: return java.lang.Long.MAX_VALUE

        return lastPublishedTimestamp * SECOND_IN_MILLIS
    }

    private suspend fun getNewerDraftPosts(maxTimestamp: Long): List<TumblrPost> = coroutineScope {
        tumblr.getDraftPosts(blogName, maxTimestamp)
    }

    suspend fun getLastPublishedTimestamp(blogName: String): LatestTimestampResult {
        val lastTimestamp = preferences.getLong(PREF_DRAFT_LAST_TIMESTAMP, -1)
        return ApiManager
            .postService()
            .getLastPublishedTimestamp(blogName, lastTimestamp)
            .response
    }

    fun refreshCache(blogName: String, last: LatestTimestampResult) {
        preferences.edit().putLong(PREF_DRAFT_LAST_TIMESTAMP, last.lastPublishTimestamp).apply()
        // delete from cache the published posts
        last.publishedIdList?.let { draftCache.delete(it, TumblrPostCache.CACHE_TYPE_DRAFT, blogName) }
    }

    private fun purgeDraftCached(
        blogName: String,
        newerDraftPosts: List<TumblrPost>,
        queuePosts: List<TumblrPost>): List<TumblrPost> {
        // update the cache with new draft posts
        draftCache.write(newerDraftPosts, TumblrPostCache.CACHE_TYPE_DRAFT)
        // delete from the cache any post moved from draft to scheduled
        draftCache.delete(queuePosts, TumblrPostCache.CACHE_TYPE_DRAFT)
        // return the updated/cleaned up cache
        return draftCache.read(blogName, TumblrPostCache.CACHE_TYPE_DRAFT)
    }

    suspend fun getDraftQueuePosts() = coroutineScope {
        val maxTimestamp = draftCache.findMostRecentTimestamp(blogName, TumblrPostCache.CACHE_TYPE_DRAFT)
        val newerDraftPostsDeferred = async(Dispatchers.IO) { getNewerDraftPosts(maxTimestamp) }
        val queuedPostsDeferred = async(Dispatchers.IO) { getQueuePosts() }
        val newerDraftPosts = newerDraftPostsDeferred.await()
        val queuedPosts = queuedPostsDeferred.await()
        DraftQueuePosts(purgeDraftCached(blogName, newerDraftPosts, queuedPosts), queuedPosts)
    }
}
