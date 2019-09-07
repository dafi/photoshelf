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
import com.ternaryop.tumblr.TumblrPost
import kotlinx.coroutines.launch

class DraftListViewModel(application: Application) : PhotoShelfViewModel<DraftListModelResult>(application) {
    private val draftPostHelper = DraftPostHelper(application)
    private var posts: List<PhotoShelfPost>? = null
    private var queuedPosts: List<TumblrPost>? = null

    fun fetchPosts(blogName: String, refresh: Boolean) {
        draftPostHelper.blogName = blogName

        if (refresh) {
            posts = null
            queuedPosts = null
        }

        viewModelScope.launch {
            try {
                if (posts == null) {
                    val latestTimestampResult = draftPostHelper.getLastPublishedTimestamp(blogName)
                    draftPostHelper.refreshCache(blogName, latestTimestampResult)
                    postResult(DraftListModelResult.FetchPosts(Command.progress(
                        ProgressData(PROGRESS_STEP_IMPORTED_POSTS, latestTimestampResult.importCount))))

                    val (cachedDraftPosts, q) = draftPostHelper.getDraftQueuePosts()

                    postResult(DraftListModelResult.FetchPosts(Command.progress(ProgressData(PROGRESS_STEP_READ_DRAFT_POSTS, 0))))
                    posts = draftPostHelper.getPhotoShelfPosts(cachedDraftPosts, q)
                    queuedPosts = q
                }
                posts?.also { p ->
                    queuedPosts?.also { q ->
                        postResult(DraftListModelResult.FetchPosts(Command.success(DraftQueuePosts(p, q))))
                    }
                }
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