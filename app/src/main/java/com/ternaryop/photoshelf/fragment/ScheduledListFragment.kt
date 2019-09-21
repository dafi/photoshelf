package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.post.PageFetcher

open class ScheduledListFragment : AbsPagingPostsListFragment() {
    private lateinit var viewModel: ScheduledListViewModel

    override val actionModeMenuId: Int
        get() = R.menu.scheduled_context

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoAdapter.counterType = CounterEvent.SCHEDULE

        viewModel = ViewModelProviders.of(this).get(ScheduledListViewModel::class.java)

        viewModel.result.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is ScheduledListResult.Scheduled -> onFetchPosts(result)
            }
        })
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        photoAdapter.setOnPhotoBrowseClick(this)

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
        viewModel.scheduled(blogName!!, params, fetchCache)
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
                result.command.error?.also { showSnackbar(makeSnake(recyclerView, it)) }
            }
            Status.PROGRESS -> {
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.scheduler, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                clearThenReloadPosts()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
