package com.ternaryop.photoshelf.util.post

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Hold position information about items read while scrolling view
 */
class OnScrollPostFetcher(private val fetcher: PostFetcher, val limitCount: Int) : RecyclerView.OnScrollListener() {
    var offset = 0
        private set
    var hasMorePosts = false
        private set
    var isScrolling = false
    var totalPosts = 0L
        private set

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        val layoutManager = recyclerView.layoutManager ?: return
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItem = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        // ignore any change after a layout calculation
        // see onScrolled documentation
        if (dx == 0 && dy == 0) {
            return
        }
        val loadMore = totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount

        if (loadMore && hasMorePosts && !isScrolling) {
            offset += limitCount
            if (!isScrolling) {
                isScrolling = true
                fetcher.fetchPosts(this)
            }
        }
    }

    fun reset() {
        offset = 0
        totalPosts = 0
        hasMorePosts = true
        isScrolling = false
    }

    fun incrementReadPostCount(count: Int) {
        if (hasMorePosts) {
            totalPosts += count
            hasMorePosts = count == limitCount
        }
    }

    interface PostFetcher {
        fun fetchPosts(listener: OnScrollPostFetcher)
    }
}
