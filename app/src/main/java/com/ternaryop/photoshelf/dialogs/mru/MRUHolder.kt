package com.ternaryop.photoshelf.dialogs.mru

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.util.mru.MRU
import com.ternaryop.utils.recyclerview.SwipeToDeleteCallback

/**
 * Created by dave on 17/05/15.
 * Allow to select items from MRU
 */
private const val MAX_TOP_ITEMS = 2
private const val MRU_TAGS_KEY = "mruTags"
private const val MRU_TAGS_MAX_SIZE = 20

class MRUHolder(private val context: Context,
    recyclerView: RecyclerView,
    private val onMRUListener: OnMRUListener): OnMRUListener {
    private val mru = MRU(context, MRU_TAGS_KEY, MRU_TAGS_MAX_SIZE)
    private val adapter = MRUAdapter(context, sortExcludingTopItems(), MAX_TOP_ITEMS)

    init {
        adapter.onMRUListener = this

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        addSwipeToDelete(recyclerView)
    }

    private fun sortExcludingTopItems(): MutableList<String> {
        val items = mutableListOf<String>().apply { addAll(mru.list) }
        if (items.size > MAX_TOP_ITEMS) {
            items.subList(MAX_TOP_ITEMS, items.size).sort()
        }
        return items
    }

    private fun addSwipeToDelete(recyclerView: RecyclerView) {
        val swipeHandler = object : SwipeToDeleteCallback(context,
            ContextCompat.getDrawable(context, R.drawable.ic_action_delete)!!,
            ColorDrawable(ContextCompat.getColor(context, R.color.animation_delete_bg))) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter.removeAt(viewHolder.adapterPosition)
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
