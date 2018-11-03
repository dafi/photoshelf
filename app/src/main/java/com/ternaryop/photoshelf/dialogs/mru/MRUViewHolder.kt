package com.ternaryop.photoshelf.dialogs.mru

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.R
import kotlin.math.absoluteValue

/**
 * Created by dave on 16/12/17.
 * MRU Holder
 */
class MRUViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val ruleView: TextView = itemView.findViewById(R.id.rule)
    private val textView: TextView = itemView.findViewById(android.R.id.text1)

    fun bindModel(item: String) {
        textView.text = item

        setColors(item)
    }

    fun setOnClickListeners(item: String, listener: View.OnClickListener?) {
        textView.setOnClickListener(listener)
        textView.tag = item
    }

    private fun setColors(item: String) {
        val textArray = itemView.context.resources.obtainTypedArray(R.array.tag_text_colors)
        val backgroundArray = itemView.context.resources.obtainTypedArray(R.array.tag_background_colors)
        val index = item.hashCode().absoluteValue % textArray.length()
        ruleView.setBackgroundColor(backgroundArray.getColor(index, 0))
        ruleView.setTextColor(textArray.getColor(index, 0))

        textArray.recycle()
        backgroundArray.recycle()
    }
}
