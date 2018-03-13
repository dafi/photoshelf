package com.ternaryop.photoshelf

import android.content.Context
import android.text.format.DateUtils.SECOND_IN_MILLIS
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.getPhotoPosts
import com.ternaryop.tumblr.queueAll
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.Locale
import java.util.concurrent.Executors

const val MAX_TAG_LAST_PUBLISHED_THREAD_POOL = 5

class DraftPostHelper(private val context: Context, private val blogName: String) {
    private val tumblr = Tumblr.getSharedTumblr(context)

    val queuePosts: Single<List<TumblrPost>>
        get() = Single.fromCallable { tumblr.queueAll(blogName) }

    /**
     * Return the map where the key is the first tag and value contains the posts for that tag
     * @param posts posts to group by tag
     * @return the (tag, posts) map
     */
    fun groupPostByTag(posts: List<TumblrPost>): Map<String, List<TumblrPost>> {
        val map = HashMap<String, MutableList<TumblrPost>>()

        for (post in posts) {
            if (post.type == "photo" && post.tags.size > 0) {
                val tag = post.tags[0].toLowerCase(Locale.US)
                var list: MutableList<TumblrPost>? = map[tag]
                if (list == null) {
                    list = mutableListOf()
                    map[tag] = list
                }
                list.add(post)
            }
        }

        return map
    }

    private fun findLastPublishedPost(tag: String): Maybe<TumblrPhotoPost> {
        return Single
                .fromCallable {
                    val params = HashMap<String, String>()
                    params["type"] = "photo"
                    params["limit"] = "1"
                    params["tag"] = tag

                    tumblr.getPhotoPosts(blogName, params)
                }
                .filter { posts -> !posts.isEmpty() }
                .map { posts -> posts[0] }
    }

    fun getTagLastPublishedMap(tags: Set<String>): Single<Map<String, Long>> {
        val postTagDAO = DBHelper.getInstance(context).postTagDAO
        val postByTags = postTagDAO.getMapTagLastPublishedTime(tags, blogName)
        val executorService = Executors.newFixedThreadPool(MAX_TAG_LAST_PUBLISHED_THREAD_POOL)

        return Observable
                .fromIterable(tags)
                .flatMap { tag ->
                    val lastPublishedTime = postByTags[tag]
                    if (lastPublishedTime == null) {
                        findLastPublishedPost(tag)
                                .subscribeOn(Schedulers.from(executorService)).toObservable()
                    } else {
                        val post = TumblrPhotoPost()
                        post.tagsFromString(tag)
                        post.timestamp = lastPublishedTime
                        Observable.just(post)
                    }
                }
                .toMap({ post -> post.tags[0].toLowerCase(Locale.US) }, { it.timestamp })
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
