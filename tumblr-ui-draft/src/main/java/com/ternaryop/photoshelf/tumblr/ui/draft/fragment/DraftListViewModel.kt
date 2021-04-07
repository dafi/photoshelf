package com.ternaryop.photoshelf.tumblr.ui.draft.fragment

import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.lifecycle.ProgressData
import com.ternaryop.photoshelf.lifecycle.ValueHolder
import com.ternaryop.photoshelf.repository.tumblr.TumblrRepository
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.draft.fragment.DraftListModelResult.Companion.PROGRESS_STEP_IMPORTED_POSTS
import com.ternaryop.photoshelf.tumblr.ui.draft.fragment.DraftListModelResult.Companion.PROGRESS_STEP_READ_DRAFT_POSTS
import com.ternaryop.photoshelf.tumblr.ui.draft.util.DraftPostHelper
import com.ternaryop.photoshelf.tumblr.ui.draft.util.DraftQueuePosts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftListViewModel @Inject constructor(
    private val tumblrRepository: TumblrRepository,
    private val draftPostHelper: DraftPostHelper
) : PhotoShelfViewModel<DraftListModelResult>() {
    private val draftQueuePosts = ValueHolder<DraftQueuePosts<PhotoShelfPost>>()

    fun fetchPosts(blogName: String) {
        val isSameBlog = draftPostHelper.blogName == blogName
        viewModelScope.launch {
            val result = draftQueuePosts.execute(isSameBlog) {
                draftPostHelper.blogName = blogName
                fetchDraftQueuePosts()
            }
            postResult(DraftListModelResult.FetchPosts(result))
        }
    }

    private suspend fun fetchDraftQueuePosts(): DraftQueuePosts<PhotoShelfPost> {
        val latestTimestampResult = draftPostHelper.getLastPublishedTimestamp()
        draftPostHelper.refreshCache(latestTimestampResult)
        postResult(DraftListModelResult.FetchPosts(Command.progress(
            ProgressData(PROGRESS_STEP_IMPORTED_POSTS, latestTimestampResult.importCount))))

        val draftQueuePosts = draftPostHelper.getDraftQueuePosts()

        postResult(DraftListModelResult.FetchPosts(Command.progress(ProgressData(PROGRESS_STEP_READ_DRAFT_POSTS, 0))))

        return draftQueuePosts.toPhotoShelfPosts(draftPostHelper.blogName)
    }

    fun clearCache() {
        draftQueuePosts.lastValue = null
    }

    fun removeFromCache(post: PhotoShelfPost) {
        draftQueuePosts.lastValue?.newerDraftPosts?.remove(post)
    }

    fun updateCount(count: Int) = tumblrRepository.updateDraftCount(count)
}

sealed class DraftListModelResult {
    data class FetchPosts(val command: Command<DraftQueuePosts<PhotoShelfPost>>) : DraftListModelResult()
    companion object {
        const val PROGRESS_STEP_IMPORTED_POSTS = 0
        const val PROGRESS_STEP_READ_DRAFT_POSTS = 1
    }
}
