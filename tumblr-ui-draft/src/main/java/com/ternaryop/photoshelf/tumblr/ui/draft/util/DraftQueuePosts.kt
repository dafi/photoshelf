package com.ternaryop.photoshelf.tumblr.ui.draft.util

import android.text.format.DateUtils
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.titlesRequestBody
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import java.util.Locale

data class DraftQueuePosts<T : TumblrPost>(val newerDraftPosts: MutableList<T>, val queuePosts: List<TumblrPost>) {
    suspend fun toPhotoShelfPosts(blogName: String): DraftQueuePosts<PhotoShelfPost> {
        val tagsForDraftPosts = groupPostByTag(newerDraftPosts)
        val tagsForQueuePosts = groupPostByTag(queuePosts)
        val lastPublished = getTagLastPublishedMap(blogName, tagsForDraftPosts.keys)
        val list = mutableListOf<PhotoShelfPost>()

        for ((tag, posts) in tagsForDraftPosts) {
            val lastPublishedTimestamp = getLastPublishedTimestampByTag(tag, lastPublished)
            val queuedTimestamp = getNextScheduledPublishTimeByTag(tag, tagsForQueuePosts)
            val timestampToSave = if (queuedTimestamp > 0) queuedTimestamp else lastPublishedTimestamp
            for (post in posts) {
                // preserve schedule time when present
                post.scheduledPublishTime = queuedTimestamp / DateUtils.SECOND_IN_MILLIS
                list += PhotoShelfPost(post as TumblrPhotoPost, timestampToSave)
            }
        }
        return DraftQueuePosts(list, queuePosts)
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

    private suspend fun getTagLastPublishedMap(blogName: String, tags: Set<String>): Map<String, Long> {
        return ApiManager.postService()
            .getMapLastPublishedTimestampTag(blogName, titlesRequestBody(tags))
            .response.pairs
    }

    private fun getNextScheduledPublishTimeByTag(tag: String, queuedPosts: Map<String, List<TumblrPost>>): Long {
        val list = queuedPosts[tag] ?: return 0

        // posts are sorted by schedule date so it's sufficient to get the first item
        return list[0].scheduledPublishTime * DateUtils.SECOND_IN_MILLIS
    }

    private fun getLastPublishedTimestampByTag(tag: String, lastPublished: Map<String, Long>): Long {
        val lastPublishedTimestamp = lastPublished[tag] ?: return java.lang.Long.MAX_VALUE

        return lastPublishedTimestamp * DateUtils.SECOND_IN_MILLIS
    }
}
