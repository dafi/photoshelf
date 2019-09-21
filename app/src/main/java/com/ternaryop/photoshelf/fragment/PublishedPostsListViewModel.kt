package com.ternaryop.photoshelf.fragment

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.util.post.FetchedData
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PublishedPostsListViewModel(application: Application) : PhotoShelfViewModel<PublishedPostsResult>(application) {
    private val tumblr = TumblrManager.getInstance(application)
    val pageFetcher = PageFetcher<PhotoShelfPost>(Tumblr.MAX_POST_PER_REQUEST)

    fun published(blogName: String, params: Map<String, String>, fetchCache: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = pageFetcher.fetch(fetchCache) {
                tumblr.getPublicPosts(blogName, params).map {
                    PhotoShelfPost(it as TumblrPhotoPost, it.timestamp * DateUtils.SECOND_IN_MILLIS)
                }
            }
            postResult(PublishedPostsResult.Published(command))
        }
    }
}

sealed class PublishedPostsResult {
    data class Published(val command: Command<FetchedData<PhotoShelfPost>>) : PublishedPostsResult()
}
