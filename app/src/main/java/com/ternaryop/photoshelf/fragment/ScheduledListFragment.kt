package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.event.CounterEvent
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher
import com.ternaryop.photoshelf.view.PhotoShelfSwipe

open class ScheduledListFragment : AbsPostsListFragment() {
    protected lateinit var photoShelfSwipe: PhotoShelfSwipe

    override val actionModeMenuId: Int
        get() = R.menu.scheduled_context

    private lateinit var viewModel: ScheduledListViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(ScheduledListViewModel::class.java)

        viewModel.result.observe(this, Observer { result ->
            when (result) {
                is ScheduledListResult.Scheduled -> onFetchPosts(result)
            }
        })

        photoAdapter.counterType = CounterEvent.SCHEDULE
        photoShelfSwipe = view.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener { resetAndReloadPhotoPosts() }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        photoAdapter.setOnPhotoBrowseClick(this)

        if (blogName != null) {
            resetAndReloadPhotoPosts()
        }
    }

    override fun fetchPosts(listener: OnScrollPostFetcher) {
        refreshUI()
        val params = HashMap<String, String>()
        params["offset"] = postFetcher.offset.toString()

        // we assume all returned items are photos, (we handle only photos)
        // if other posts type are returned, the getQueue() list size may be greater than photo list size
        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.scheduled(blogName!!, params, false)
    }

    private fun onFetchPosts(result: ScheduledListResult.Scheduled) {
        postFetcher.isScrolling = false
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.also { fetched ->
                    photoShelfSwipe.setRefreshingAndWaitingResult(false)
                    postFetcher.incrementReadPostCount(fetched.lastFetchCount)
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
                resetAndReloadPhotoPosts()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
