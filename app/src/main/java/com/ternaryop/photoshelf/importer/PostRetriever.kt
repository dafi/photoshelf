package com.ternaryop.photoshelf.importer

import android.content.Context
import com.ternaryop.photoshelf.util.network.ApiManager
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import io.reactivex.Observable

class PostRetriever(context: Context) {
    private val sharedTumblr = TumblrManager.getInstance(context)
    private val postManager = ApiManager.postManager(context)
    private var offset: Int = 0
    var total: Int = 0
        private set

    fun readPhotoPosts(tumblrName: String,
        lastPublishTimestamp: Long, tag: String? = null): Observable<List<TumblrPost>> {
        val params = buildParams(tag)

        offset = 0
        total = 0
        return Observable.generate { emitter ->
            if (offset == 0) {
                // TODO: Use PostManager API to update data on server, this will replace readPhotoPosts when migration from sqlite will be completed
                postManager.getLastPublishedTimestamp(tumblrName, lastPublishTimestamp)
            }
            params["offset"] = offset.toString()
            val postsList = sharedTumblr.getPublicPosts(tumblrName, params)
            var loadNext = postsList.isNotEmpty()
            offset += postsList.size

            val newerPosts = mutableListOf<TumblrPost>()
            for (tumblrPost in postsList) {
                if (lastPublishTimestamp < tumblrPost.timestamp) {
                    newerPosts.add(tumblrPost)
                } else {
                    loadNext = false
                    break
                }
            }

            total += newerPosts.size

            emitter.onNext(newerPosts)
            if (!loadNext) {
                emitter.onComplete()
            }
        }
    }

    private fun buildParams(tag: String?): HashMap<String, String> {
        val params = HashMap<String, String>()
        params["type"] = "photo"
        if (tag != null && !tag.trim { it <= ' ' }.isEmpty()) {
            params["tag"] = tag
        }
        return params
    }
}