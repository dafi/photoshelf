package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.tumblr.ui.core.R

class PhotoGridAdapter(
    context: Context
) : PhotoAdapter<PhotoGridViewHolder>(context) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoGridViewHolder {
        return PhotoGridViewHolder(LayoutInflater.from(context).inflate(R.layout.grid_photo_item, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoGridViewHolder, position: Int) {
        val item = getItem(position)

        holder.bindModel(item, selection.isSelected(position))

        if (onPhotoBrowseClick == null) {
            return
        }
        holder.setOnClickTags(this)
        holder.setOnClickMenu(this)
        holder.setOnClickThumbnail(this, this)
    }

    override fun onClick(v: View) {
        val onPhotoBrowseClick = onPhotoBrowseClick ?: return
        when (v.id) {
            R.id.menu -> onPhotoBrowseClick.onOverflowClick(v.tag as Int, v)
            R.id.grid_photo_item -> if (isActionModeOn) {
                (onPhotoBrowseClick as OnPhotoBrowseClickMultiChoice).onItemClick(v.tag as Int)
            } else {
                onPhotoBrowseClick.onThumbnailImageClick(v.tag as Int)
            }
            R.id.tag_text_view -> {
                val position = (v.parent as ViewGroup).tag as Int
                onPhotoBrowseClick.onTagClick(position, v.tag as String)
            }
        }
    }
}