package com.ternaryop.photoshelf.adapter.mru

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.R

/**
 * Created by dave on 16/12/17.
 * MRU Holder
 */
class MRUViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val ruleView: TextView = itemView.findViewById(R.id.rule)
    private val textView: TextView = itemView.findViewById(android.R.id.text1)

    fun bindModel(item: String, maxTopItems: Int) {
        textView.text = item

        setColors(maxTopItems)
    }

    fun setOnClickListeners(item: String, listener: View.OnClickListener?) {
        itemView.setOnClickListener(listener)
        itemView.tag = item
    }

    private fun setColors(maxTopItems: Int) {
        if (adapterPosition < maxTopItems) {
            ruleView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.mru_top_item_bg))
        } else {
            ruleView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.mru_other_item_bg))
        }
    }
}
