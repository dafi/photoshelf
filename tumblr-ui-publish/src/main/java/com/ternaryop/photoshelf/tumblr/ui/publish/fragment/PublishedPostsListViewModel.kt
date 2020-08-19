package com.ternaryop.photoshelf.tumblr.ui.publish.fragment

import android.text.format.DateUtils
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.post.FetchedData
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PublishedPostsListViewModel @ViewModelInject constructor(
    private val tumblr: Tumblr
) : PhotoShelfViewModel<PublishedPostsResult>() {
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
