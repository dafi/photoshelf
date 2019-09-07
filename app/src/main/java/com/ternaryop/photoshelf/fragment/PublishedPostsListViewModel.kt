package com.ternaryop.photoshelf.fragment

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class FetchedPosts(val list: List<PhotoShelfPost>, val lastFetchCount: Int)

class PublishedPostsListViewModel(application: Application) : PhotoShelfViewModel<PublishedPostsResult>(application) {
    private val tumblr = TumblrManager.getInstance(application)
    private var publishedList: MutableList<PhotoShelfPost>? = null

    fun published(blogName: String, params: Map<String, String>, refresh: Boolean) {
        if (refresh) {
            publishedList = null
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (publishedList == null) {
                    publishedList = mutableListOf()
                }
                val list = tumblr.getPublicPosts(blogName, params).map {
                    PhotoShelfPost(it as TumblrPhotoPost, it.timestamp * DateUtils.SECOND_IN_MILLIS)
                }
                publishedList?.apply {
                    addAll(list)

                    postResult(PublishedPostsResult.Published(Command.success(FetchedPosts(this, list.size))))
                }
            } catch (t: Throwable) {
                postResult(PublishedPostsResult.Published(Command.error(t)))
            }
        }
    }
}

sealed class PublishedPostsResult {
    data class Published(val command: Command<FetchedPosts>) : PublishedPostsResult()
}
