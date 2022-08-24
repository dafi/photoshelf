package com.ternaryop.photoshelf.tumblr.ui.schedule.fragment

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.switcher.AdapterSwitcherConfig
import com.ternaryop.photoshelf.tumblr.ui.core.fragment.AbsPagingPostsListFragment
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.PostAction
import com.ternaryop.photoshelf.tumblr.ui.core.postaction.showConfirmDialog
import com.ternaryop.photoshelf.tumblr.ui.schedule.R
import com.ternaryop.photoshelf.util.post.PageFetcher
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
open class ScheduledListFragment(
    iav: ImageViewerActivityStarter,
    pd: TumblrPostDialog
) : AbsPagingPostsListFragment(iav, pd) {
    private val viewModel: ScheduledListViewModel by viewModels()
    override val actionModeMenuId: Int
        get() = R.menu.scheduled_context

    override val actionBarGroupMenuId: Int
        get() = R.id.menu_photo_action_bar

    override val adapterSwitcherConfig: AdapterSwitcherConfig
        get() = AdapterSwitcherConfig("ScheduledList", false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoAdapter.onPhotoBrowseClick = this
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.post_future_background_color))

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is ScheduledListResult.Scheduled -> onFetchPosts(result)
            }
        })

        if (blogName != null) {
            fetchPosts(true)
        }
    }

    override val pageFetcher: PageFetcher<PhotoShelfPost>
        get() = viewModel.pageFetcher

    override fun fetchPosts(fetchCache: Boolean) {
        refreshUI()
        photoShelfSwipe.setRefreshingAndWaitingResult(true)

        val params = mapOf(
            "offset" to pageFetcher.pagingInfo.offset.toString())

        // we assume all returned items are photos, (we handle only photos)
        // if other posts type are returned, the getQueue() list size may be greater than photo list size
        viewModel.scheduled(requireBlogName, params, fetchCache)
    }

    private fun onFetchPosts(result: ScheduledListResult.Scheduled) {
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

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.scheduler, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                clearThenReloadPosts()
                true
            }
            else -> super.onMenuItemSelected(item)
        }
    }

    protected open fun updateCount() = viewModel.updateCount(photoAdapter.itemCount)

    override fun updateTitleBar() {
        updateCount()
        super.updateTitleBar()
    }

    override fun handleMenuItem(item: MenuItem, postList: List<PhotoShelfPost>, mode: ActionMode?): Boolean {
        val blogName = blogName ?: return false

        return when (item.itemId) {
            R.id.post_publish -> {
                PostAction.Publish(blogName, postList).showConfirmDialog(requireContext(), onConfirm)
                true
            }
            R.id.post_save_draft -> {
                PostAction.SaveAsDraft(blogName, postList).showConfirmDialog(requireContext(), onConfirm)
                true
            }
            else -> super.handleMenuItem(item, postList, mode)
        }
    }
}
