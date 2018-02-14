package com.ternaryop.photoshelf.adapter

/**
 * Created by dave on 13/04/16.
 * The selection list
 */
interface Selection {
    val itemCount: Int

    val selectedPositions: IntArray
    fun isSelected(position: Int): Boolean
    fun toggle(position: Int)
    fun setSelected(position: Int, selected: Boolean)
    fun setSelectedRange(start: Int, end: Int, selected: Boolean)
    fun clear()
}
