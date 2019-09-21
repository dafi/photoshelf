package com.ternaryop.photoshelf.fragment

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.Response
import com.ternaryop.photoshelf.api.post.TagInfoListResult
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.util.post.FetchedData
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getPhotoPosts
import com.ternaryop.util.coroutine.ControlledRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TagPhotoBrowserViewModel(application: Application) : PhotoShelfViewModel<TagPhotoBrowserResult>(application) {
    private val tumblr = TumblrManager.getInstance(application)
    private val controlledRunner = ControlledRunner<Response<TagInfoListResult>>()
    val pageFetcher = PageFetcher<PhotoShelfPost>(Tumblr.MAX_POST_PER_REQUEST)

    fun findTags(blogName: String, pattern: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // cancelPreviousThenRun ensures only the last request is performed so the found result is reliable
                val tags = controlledRunner.cancelPreviousThenRun {
                    ApiManager.postService().findTags(blogName, pattern)
                }
                postResult(TagPhotoBrowserResult.FindTags(Command.success(tags.response)))
            } catch (ignored: CancellationException) {
            } catch (t: Throwable) {
                postResult(TagPhotoBrowserResult.FindTags(Command.error(t)))
            }
        }
    }

    fun photos(blogName: String, params: Map<String, String>, fetchCache: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = pageFetcher.fetch(fetchCache) {
                tumblr.getPhotoPosts(blogName, params).map { tumblrPost ->
                    PhotoShelfPost(tumblrPost, tumblrPost.timestamp * DateUtils.SECOND_IN_MILLIS)
                }
            }
            postResult(TagPhotoBrowserResult.Photos(command))
        }
    }
}

sealed class TagPhotoBrowserResult {
    data class FindTags(val command: Command<TagInfoListResult>) : TagPhotoBrowserResult()
    data class Photos(val command: Command<FetchedData<PhotoShelfPost>>) : TagPhotoBrowserResult()
}
