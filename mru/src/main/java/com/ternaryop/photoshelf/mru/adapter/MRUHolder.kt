package com.ternaryop.photoshelf.mru.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.mru.MRU
import com.ternaryop.photoshelf.mru.R
import com.ternaryop.utils.recyclerview.SwipeCallback

/**
 * Created by dave on 17/05/15.
 * Allow to select items from MRU
 */
private const val MRU_TAGS_KEY = "mruTags"

class MRUHolder(
    private val context: Context,
    recyclerView: RecyclerView,
    maxMruItems: Int,
    maxHighlightedItems: Int,
    private val onMRUListener: OnMRUListener
) : OnMRUListener {
    private val mru = MRU(context, MRU_TAGS_KEY, maxMruItems)
    private val adapter = MRUAdapter(context, sortExcludingHighlightedItems(maxHighlightedItems), maxHighlightedItems)

    init {
        adapter.onMRUListener = this

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        addSwipeToDelete(recyclerView)
    }

    private fun sortExcludingHighlightedItems(maxHighlightedItems: Int): MutableList<String> {
        val items = mutableListOf<String>().apply { addAll(mru.list) }
        if (items.size > maxHighlightedItems) {
            items.subList(maxHighlightedItems, items.size).sort()
        }
        return items
    }

    private fun addSwipeToDelete(recyclerView: RecyclerView) {
        val swipeHandler = object : SwipeCallback(
            checkNotNull(ContextCompat.getDrawable(context, R.drawable.ic_action_delete)),
            ColorDrawable(ContextCompat.getColor(context, R.color.animation_delete_bg))) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter.removeAt(viewHolder.bindingAdapterPosition)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(recyclerView)
    }

    fun updateMruList(tags: List<String>) {
        mru.add(tags)
        mru.save()
    }

    override fun onItemSelect(item: String) {
        mru.add(item)
        onMRUListener.onItemSelect(item)
    }

    override fun onItemDelete(item: String) {
        mru.remove(item)
        mru.save()
        onMRUListener.onItemDelete(item)
    }
}
