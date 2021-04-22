package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ternaryop.photoshelf.adapter.OnPhotoBrowseClickMultiChoice
import com.ternaryop.photoshelf.tumblr.ui.core.R

class PhotoListRowAdapter(
    context: Context,
    private val thumbnailWidth: Int
) : PhotoAdapter<PhotoViewHolder>(context) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        return PhotoViewHolder(LayoutInflater.from(context).inflate(R.layout.list_row, parent, false))
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val listener = if (onPhotoBrowseClick == null) null else this
        val showUploadTime = sortSwitcher.sortable.sortId == PhotoSortSwitcher.UPLOAD_TIME
        val post = visiblePosts[position]
        holder.bindModel(post, thumbnailWidth, showUploadTime)
        holder.setOnClickListeners(listener)
        if (onPhotoBrowseClick is OnPhotoBrowseClickMultiChoice) {
            holder.setOnClickMultiChoiceListeners(listener, this)
        }
        holder.itemView.isSelected = selection.isSelected(position)
    }

    override fun onClick(v: View) {
        val onPhotoBrowseClick = onPhotoBrowseClick ?: return
        when (v.id) {
            R.id.thumbnail_image -> onPhotoBrowseClick.onThumbnailImageClick(v.tag as Int)
            R.id.menu -> onPhotoBrowseClick.onOverflowClick(v.tag as Int, v)
            R.id.list_row -> (onPhotoBrowseClick as OnPhotoBrowseClickMultiChoice).onItemClick(v.tag as Int)
            R.id.tag_text_view -> {
                val position = (v.parent as ViewGroup).tag as Int
                onPhotoBrowseClick.onTagClick(position, v.tag as String)
            }
        }
    }
}