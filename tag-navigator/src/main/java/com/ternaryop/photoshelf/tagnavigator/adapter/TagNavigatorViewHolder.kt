package com.ternaryop.photoshelf.tagnavigator.adapter

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.api.post.TagInfo
import com.ternaryop.photoshelf.tagnavigator.R
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.htmlHighlightPattern

class TagNavigatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val tagView: TextView = itemView.findViewById(R.id.tag)
    private val countView: TextView = itemView.findViewById(R.id.count)

    fun bindModel(tagInfo: TagInfo, pattern: CharSequence?) {
        tagView.text = if (pattern == null || pattern.isBlank()) {
            tagInfo.tag
        } else {
            tagInfo.tag.htmlHighlightPattern(pattern.toString()).fromHtml()
        }
        countView.text = String.format("%3d", tagInfo.postCount)
    }

    fun setOnClickListeners(listener: View.OnClickListener) {
        itemView.setOnClickListener(listener)
        itemView.tag = adapterPosition
    }
}
