package com.ternaryop.photoshelf.tumblr.dialog.editor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.ternaryop.photoshelf.adapter.AbsBaseAdapter
import com.ternaryop.photoshelf.tumblr.dialog.R

class ThumbnailAdapter(
    private val context: Context,
    private val thumbnailSize: Int
) : AbsBaseAdapter<ThumbnailViewHolder>() {
    private val items: MutableList<String> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        return ThumbnailViewHolder(LayoutInflater.from(context).inflate(R.layout.thumbnail, parent, false))
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        holder.bindModel(items[position], thumbnailSize)
    }

    override fun getItemCount() = items.size

    fun getItem(position: Int) = items[position]

    fun addAll(list: List<String>) {
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun clear() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }
}
