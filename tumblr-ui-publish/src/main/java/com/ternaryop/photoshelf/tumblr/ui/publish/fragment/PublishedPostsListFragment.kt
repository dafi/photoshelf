package com.ternaryop.photoshelf.tumblr.ui.publish.fragment

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcherConfig
import com.ternaryop.photoshelf.tumblr.ui.publish.R
import com.ternaryop.photoshelf.tumblr.ui.schedule.fragment.ScheduledListFragment
import com.ternaryop.photoshelf.util.post.PageFetcher

class PublishedPostsListFragment(
    iav: ImageViewerActivityStarter,
    pd: TumblrPostDialog
) : ScheduledListFragment(iav, pd) {
    override val actionModeMenuId: Int
        get() = R.menu.published_context
    private val viewModel: PublishedPostsListViewModel by viewModels()

    override val adapterSwitcherConfig: AdapterSwitcherConfig
        get() = AdapterSwitcherConfig("PublishedPostsList", false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(),
                com.ternaryop.photoshelf.core.R.color.post_even_background_color
            )
        )
        viewModel.result.observe(
            viewLifecycleOwner,
            EventObserver { result ->
                when (result) {
                    is PublishedPostsResult.Published -> onFetchPosts(result)
                }
            }
        )
    }

    override val pageFetcher: PageFetcher<PhotoShelfPost>
        get() = viewModel.pageFetcher

    override fun fetchPosts(fetchCache: Boolean) {
        refreshUI()
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        val params = mapOf(
            "offset" to pageFetcher.pagingInfo.offset.toString(),
            "type" to "photo",
            "notes_info" to "true"
        )
        viewModel.published(requireBlogName, params, fetchCache)
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
                snackbarHolder.show(recyclerView, result.command.error)
            }
            Status.PROGRESS -> {
            }
        }
    }

    override fun updateCount() {
        // do nothing otherwise the schedule badge is incorrectly updated
    }
}
