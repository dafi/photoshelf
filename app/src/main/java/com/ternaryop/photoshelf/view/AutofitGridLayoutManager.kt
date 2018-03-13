package com.ternaryop.photoshelf.view

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue

/**
 * Copied from http://stackoverflow.com/a/30256880/195893
 */
class AutofitGridLayoutManager : GridLayoutManager {
    private var mColumnWidth: Int = 0
    private var mColumnWidthChanged = true

    /* Initially set spanCount to 1, will be changed automatically later. */
    constructor(context: Context, columnWidth: Int) : super(context, 1) {
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    /* Initially set spanCount to 1, will be changed automatically later. */
    constructor(context: Context, columnWidth: Int, orientation: Int, reverseLayout: Boolean)
        : super(context, 1, orientation, reverseLayout) {
        setColumnWidth(checkedColumnWidth(context, columnWidth))
    }

    private fun checkedColumnWidth(context: Context, columnWidth: Int): Int {
        return if (columnWidth <= 0) {
            /* Set default columnWidth value (48dp here). It is better to move this constant
            to static constant on top, but we need context to convert it to dp, so can't really
            do so. */
            pixel2DP(context, defaultColumnPixelWidth)
        } else {
            columnWidth
        }
    }

    private fun setColumnWidth(newColumnWidth: Int) {
        if (newColumnWidth > 0 && newColumnWidth != mColumnWidth) {
            mColumnWidth = newColumnWidth
            mColumnWidthChanged = true
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        if (mColumnWidthChanged && mColumnWidth > 0) {
            val totalSpace = if (orientation == LinearLayoutManager.VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            val spanCount = Math.max(1, totalSpace / mColumnWidth)
            setSpanCount(spanCount)
            mColumnWidthChanged = false
        }
        super.onLayoutChildren(recycler, state)
    }

    companion object {
        private const val defaultColumnPixelWidth = 48

        fun pixel2DP(context: Context, pixel: Int): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixel.toFloat(),
                    context.resources.displayMetrics).toInt()
        }
    }
}