package com.ternaryop.photoshelf.fragment.draft

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ternaryop.photoshelf.DraftPostHelper
import com.ternaryop.photoshelf.DraftQueuePosts
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.fragment.draft.DraftListModelResult.Companion.PROGRESS_STEP_IMPORTED_POSTS
import com.ternaryop.photoshelf.fragment.draft.DraftListModelResult.Companion.PROGRESS_STEP_READ_DRAFT_POSTS
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.lifecycle.ProgressData
import kotlinx.coroutines.launch

class DraftListViewModel(application: Application) : PhotoShelfViewModel<DraftListModelResult>(application) {
    private val draftPostHelper = DraftPostHelper(application)

    fun fetchPosts(blogName: String) {
        draftPostHelper.blogName = blogName

        viewModelScope.launch {
            try {
                val latestTimestampResult = draftPostHelper.getLastPublishedTimestamp(blogName)
                draftPostHelper.refreshCache(blogName, latestTimestampResult)
                postResult(DraftListModelResult.FetchPosts(Command.progress(
                    ProgressData(PROGRESS_STEP_IMPORTED_POSTS, latestTimestampResult.importCount))))

                val (cachedDraftPosts, queuedPosts) = draftPostHelper.getDraftQueuePosts()

                postResult(DraftListModelResult.FetchPosts(Command.progress(ProgressData(PROGRESS_STEP_READ_DRAFT_POSTS, 0))))

                val posts = draftPostHelper.getPhotoShelfPosts(cachedDraftPosts, queuedPosts)

                postResult(DraftListModelResult.FetchPosts(Command.success(DraftQueuePosts(posts, queuedPosts))))
            } catch (t: Throwable) {
                postResult(DraftListModelResult.FetchPosts(Command.error(t)))
            }
        }
    }
}

sealed class DraftListModelResult {
    data class FetchPosts(val command: Command<DraftQueuePosts<PhotoShelfPost>>) : DraftListModelResult()
    companion object {
        const val PROGRESS_STEP_IMPORTED_POSTS = 0
        const val PROGRESS_STEP_READ_DRAFT_POSTS = 1
    }
}