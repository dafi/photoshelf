package com.ternaryop.photoshelf.dialogs.mru

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView

/**
 * Created by dave on 16/12/17.
 * MRU Holder
 */
class MRUViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textView: TextView = itemView.findViewById(android.R.id.text1)

    fun bindModel(item: String) {
        textView.text = item
    }

    fun setOnClickListeners(item: String, listener: View.OnClickListener?) {
        textView.setOnClickListener(listener)
        textView.tag = item
    }
}
