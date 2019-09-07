package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.lifecycle.Status
import com.ternaryop.photoshelf.util.post.OnScrollPostFetcher

class PublishedPostsListFragment : ScheduledListFragment() {
    override val actionModeMenuId: Int
        get() = R.menu.published_context

    private lateinit var viewModel: PublishedPostsListViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProviders.of(this).get(PublishedPostsListViewModel::class.java)

        viewModel.result.observe(this, Observer { result ->
            when (result) {
                is PublishedPostsResult.Published -> onFetchPosts(result)
            }
        })

    }

    override fun fetchPosts(listener: OnScrollPostFetcher) {
        refreshUI()

        val params = HashMap<String, String>()
        params["offset"] = postFetcher.offset.toString()
        params["type"] = "photo"
        params["notes_info"] = "true"

        photoShelfSwipe.setRefreshingAndWaitingResult(true)
        viewModel.published(blogName!!, params, false)
    }

    private fun onFetchPosts(result: PublishedPostsResult.Published) {
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
}
