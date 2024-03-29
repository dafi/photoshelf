package com.ternaryop.photoshelf.tumblr.ui.core.fragment

import android.os.Bundle
import android.view.View
import com.ternaryop.photoshelf.activity.ImageViewerActivityStarter
import com.ternaryop.photoshelf.tumblr.dialog.TumblrPostDialog
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.post.OnPagingScrollListener
import com.ternaryop.photoshelf.util.post.PageFetcher
import com.ternaryop.photoshelf.util.post.removeItem
import com.ternaryop.photoshelf.view.PhotoShelfSwipe

abstract class AbsPagingPostsListFragment(
    iav: ImageViewerActivityStarter,
    pd: TumblrPostDialog
) : AbsPostsListFragment(iav, pd), OnPagingScrollListener.OnScrollListener {
    protected lateinit var photoShelfSwipe: PhotoShelfSwipe

    abstract val pageFetcher: PageFetcher<PhotoShelfPost>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoShelfSwipe = view.findViewById(R.id.swipe_container)
        photoShelfSwipe.setOnRefreshListener { clearThenReloadPosts() }

        recyclerView.addOnScrollListener(OnPagingScrollListener(this))

        photoAdapter.onPhotoBrowseClick = this
    }

    protected open fun clearThenReloadPosts() {
        pageFetcher.clear()
        photoAdapter.clear()
        fetchPosts(false)
    }

    override fun onScrolled(
        onPagingScrollListener: OnPagingScrollListener,
        firstVisibleItem: Int,
        visibleItemCount: Int,
        totalItemCount: Int
    ) {
        if (pageFetcher.changedScrollPosition(firstVisibleItem, visibleItemCount, totalItemCount)) {
            fetchPosts(false)
        }
    }

    override fun updateTitleBar() {
        if (pageFetcher.pagingInfo.hasMoreItems) {
            supportActionBar?.subtitle = getString(
                R.string.post_count_1_of_x,
                photoAdapter.itemCount,
                pageFetcher.pagingInfo.totalItems
            )
        } else {
            super.updateTitleBar()
        }
    }

    abstract fun fetchPosts(fetchCache: Boolean)

    override fun removeFromCache(post: PhotoShelfPost) {
        pageFetcher.removeItem(post)
    }
}
