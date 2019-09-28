package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProviders
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.post.PageFetcher

class PublishedPostsListFragment : ScheduledListFragment() {
    override val actionModeMenuId: Int
        get() = R.menu.published_context
    private lateinit var viewModel: PublishedPostsListViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(PublishedPostsListViewModel::class.java)

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is PublishedPostsResult.Published -> onFetchPosts(result)
            }
        })
    }

    override val pageFetcher: PageFetcher<PhotoShelfPost>
        get() = viewModel.pageFetcher

    override fun fetchPosts(fetchCache: Boolean) {
        refreshUI()
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        val params = mapOf(
            "offset" to pageFetcher.pagingInfo.offset.toString(),
            "type" to "photo",
            "notes_info" to "true")
        viewModel.published(blogName!!, params, fetchCache)
    }

    private fun onFetchPosts(result: PublishedPostsResult.Published) {
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { fetched ->
                    photoShelfSwipe.setRefreshingAndWaitingResult(false)
                    photoAdapter.setPosts(fetched.list)
                    refreshUI()
                }
            }
            Status.ERROR -> {
                photoShelfSwipe.setRefreshingAndWaitingResult(false)
                result.command.error?.also { showSnackbar(makeSnake(recyclerView, it)) }
            }
            Status.PROGRESS -> {
            }
        }
    }
}
