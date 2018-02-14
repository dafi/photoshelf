package com.ternaryop.photoshelf.adapter

import android.util.SparseBooleanArray

/**
 * Created by dave on 13/04/16.
 * Hold selection index state
 */
open class SelectionArray : Selection {
    private val items = SparseBooleanArray()

    override val itemCount: Int
        get() = items.size()

    override val selectedPositions: IntArray
        get() {
            val positions = IntArray(items.size())

            for (i in 0 until items.size()) {
                positions[i] = items.keyAt(i)
            }

            return positions
        }

    override fun toggle(position: Int) {
        if (items.get(position, false)) {
            items.delete(position)
        } else {
            items.put(position, true)
        }
    }

    override fun isSelected(position: Int): Boolean {
        return items.get(position, false)
    }

    override fun setSelected(position: Int, selected: Boolean) {
        if (selected) {
            items.put(position, true)
        } else {
            items.delete(position)
        }
    }

    override fun clear() {
        items.clear()
    }

    override fun setSelectedRange(start: Int, end: Int, selected: Boolean) {
        for (i in start until end) {
            setSelected(i, selected)
        }
    }
}
