package com.ternaryop.photoshelf.util.post

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OnPagingScrollListener(private val onScrollListener: OnScrollListener) : RecyclerView.OnScrollListener() {
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

        onScrollListener.onScrolled(this, firstVisibleItem, visibleItemCount, totalItemCount)
    }

    interface OnScrollListener {
        fun onScrolled(
            onPagingScrollListener: OnPagingScrollListener,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int)
    }
}