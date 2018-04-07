package com.ternaryop.photoshelf.util.post

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.ternaryop.tumblr.Tumblr

/**
 * Hold position information about posts read while scrolling view
 */
class OnScrollPostFetcher(private val fetcher: PostFetcher) : RecyclerView.OnScrollListener() {
    var offset = 0
        private set
    var hasMorePosts = false
        private set
    var isScrolling = false
    var totalPosts = 0L
        private set

    override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
        val layoutManager = recyclerView!!.layoutManager
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItem = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

        val loadMore = totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount

        if (loadMore && hasMorePosts && !isScrolling) {
            offset += Tumblr.MAX_POST_PER_REQUEST
            if (!isScrolling) {
                isScrolling = true
                fetcher.fetchPosts()
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
        totalPosts += count
        hasMorePosts = count == Tumblr.MAX_POST_PER_REQUEST
    }

    interface PostFetcher {
        fun fetchPosts()
    }
}
