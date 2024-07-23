package com.ternaryop.photoshelf.feedly.adapter

/**
 * Hold the id (single selection) shown as selected on list view
 */
class SelectedItem(
    private val adapter: FeedlyContentAdapter
) {
    var itemId: String? = null
        private set

    fun clear() {
        itemId?.let {
            val position = adapter.getPositionById(it)
            if (position != -1) {
                setSelectionAndNotify(position, false)
            }
        }
    }

    fun setSelected(itemId: String) {
        val position = adapter.getPositionById(itemId)

        if (position == -1) {
            this.itemId = null
        } else {
            this.itemId = itemId
            setSelectionAndNotify(position, true)
        }
    }

    private fun setSelectionAndNotify(position: Int, isSelected: Boolean) {
        adapter.getItem(position).isSelected = isSelected
        adapter.notifyItemChanged(position)
    }
}