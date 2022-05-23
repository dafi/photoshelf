package com.ternaryop.photoshelf.feedly.fragment

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentAdapter
import com.ternaryop.utils.recyclerview.SwipeCallback

typealias OnItemSwiped = (position: Int, isChecked: Boolean) -> Unit

internal fun RecyclerView.addToggleSwipe(onItemSwiped: OnItemSwiped) {
    val swipeHandler = FeedlyContentSwipeCallback(context, adapter as FeedlyContentAdapter, onItemSwiped)
    ItemTouchHelper(swipeHandler).attachToRecyclerView(this)
}

internal class FeedlyContentSwipeCallback(
    private val context: Context,
    private val adapter: FeedlyContentAdapter,
    private val onItemSwiped: OnItemSwiped
) : SwipeCallback(
    checkNotNull(ContextCompat.getDrawable(context, R.drawable.bookmark_green)),
    ColorDrawable(ContextCompat.getColor(context, R.color.swipe_toggle))
) {
    override fun prepareIconBeforeDraw(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Drawable? {
        val position = viewHolder.bindingAdapterPosition
        if (position < 0) {
            return null
        }
        val item = adapter.getItem(position)
        val iconRes = if (item.isChecked) {
            R.drawable.bookmark_border
        } else {
            R.drawable.bookmark_green
        }
        return ContextCompat.getDrawable(context, iconRes)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        val item = adapter.getItem(position)
        item.isChecked = !item.isChecked
        onItemSwiped(position, item.isChecked)
    }
}
