package com.ternaryop.photoshelf.tagphotobrowser.fragment

import android.text.format.DateUtils
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.Response
import com.ternaryop.photoshelf.api.post.TagInfoListResult
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.post.FetchedData
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.getPhotoPosts
import com.ternaryop.util.coroutine.ControlledRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagPhotoBrowserViewModel @Inject constructor(
    private val tumblr: Tumblr
) : PhotoShelfViewModel<TagPhotoBrowserResult>() {
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
            } catch (expected: Throwable) {
                postResult(TagPhotoBrowserResult.FindTags(Command.error(expected)))
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
