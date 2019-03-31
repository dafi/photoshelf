package com.ternaryop.photoshelf

import android.content.Context
import android.text.format.DateUtils.SECOND_IN_MILLIS
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.api.post.titlesRequestBody
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getDraftPosts
import com.ternaryop.tumblr.queueAll
import io.reactivex.Single
import java.util.Locale

class DraftPostHelper(private val context: Context, private val blogName: String) {
    private val tumblr = TumblrManager.getInstance(context)

    val queuePosts: Single<List<TumblrPost>>
        get() = Single.fromCallable { tumblr.queueAll(blogName) }

    /**
     * Return the map where the key is the first tag and value contains the posts for that tag
     * @param posts posts to group by tag
     * @return the (tag, posts) map
     */
    fun groupPostByTag(posts: List<TumblrPost>): Map<String, List<TumblrPost>> {
        return posts
            .filter { it.tags.isNotEmpty() && it.type == "photo" }
            .groupBy { it.tags[0].toLowerCase(Locale.US) }
    }

    fun getTagLastPublishedMap(tags: Set<String>): Single<Map<String, Long>> {
        return ApiManager.postService()
            .getMapLastPublishedTimestampTag(blogName, titlesRequestBody(tags))
            .map { result -> result.response.pairs }
    }

    fun getPhotoShelfPosts(
        draftPosts: Map<String, List<TumblrPost>>,
        queuedPosts: Map<String, List<TumblrPost>>,
        lastPublished: Map<String, Long>): List<PhotoShelfPost> {
        val list = mutableListOf<PhotoShelfPost>()

        for ((tag, posts) in draftPosts) {
            val lastPublishedTimestamp = getLastPublishedTimestampByTag(tag, lastPublished)
            val queuedTimestamp = getNextScheduledPublishTimeByTag(tag, queuedPosts)
            val timestampToSave = if (queuedTimestamp > 0) queuedTimestamp else lastPublishedTimestamp
            for (post in posts) {
                // preserve schedule time when present
                post.scheduledPublishTime = queuedTimestamp / SECOND_IN_MILLIS
                list.add(PhotoShelfPost(post as TumblrPhotoPost, timestampToSave))
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

    fun getNewerDraftPosts(maxTimestamp: Long): Single<List<TumblrPost>> {
        return Single.fromCallable { tumblr.getDraftPosts(blogName, maxTimestamp) }
    }
}
