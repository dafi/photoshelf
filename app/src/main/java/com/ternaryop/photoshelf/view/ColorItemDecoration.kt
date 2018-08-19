package com.ternaryop.photoshelf.view

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

/**
 * Set the background color for the item while an animation is running
 * Created by dave on 13/08/17.
 */

class ColorItemDecoration : RecyclerView.ItemDecoration() {
    private val background: ColorDrawable = ColorDrawable(Color.TRANSPARENT)

    fun setColor(@ColorInt color: Int) {
        background.color = color
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(c, parent, state)

        // change the color only while animation is running
        if (parent.itemAnimator == null || !parent.itemAnimator!!.isRunning) {
            return
        }
        val layoutManager = parent.layoutManager ?: return
        val left = 0
        val right = parent.width
        val childCount = layoutManager.childCount

        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i)!!
            val top = child.top
            val bottom = child.bottom

            background.setBounds(left, top, right, bottom)
            background.draw(c)
        }
    }
}
