package com.ternaryop.photoshelf.adapter

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by dave on 13/04/16.
 *
 * Notify changes to the adapter delegate
 */
class SelectionArrayViewHolder<T : RecyclerView.ViewHolder>(
    private val adapter: RecyclerView.Adapter<T>
) : SelectionArray() {

    override fun toggle(position: Int) {
        super.toggle(position)
        adapter.notifyItemChanged(position)
    }

    override fun setSelected(position: Int, selected: Boolean) {
        super.setSelected(position, selected)
        adapter.notifyItemChanged(position)
    }

    override fun clear() {
        super.clear()
        adapter.notifyItemRangeChanged(0, adapter.itemCount)
    }

    override fun setSelectedRange(start: Int, end: Int, selected: Boolean) {
        super.setSelectedRange(start, end, selected)
        adapter.notifyDataSetChanged()
    }
}
