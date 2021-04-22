package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.target.ImageViewTarget
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.widget.TagListLayout
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.widget.CheckableImageView

class PhotoGridViewHolder(vi: View) : RecyclerView.ViewHolder(vi) {
    private val tagList: TagListLayout = itemView.findViewById(R.id.tags_container)
    val thumbImage = vi.findViewById<View>(R.id.thumbnail_image) as CheckableImageView
    val menu = vi.findViewById<View>(R.id.menu) as ImageView
    private lateinit var post: PhotoShelfPost

    fun bindModel(post: PhotoShelfPost, checked: Boolean) {
        this.post = post
        tagList.addTags(post.tags)
        displayImage(checked)
    }

    private fun displayImage(checked: Boolean) {
        thumbImage.load(checkNotNull(post.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_250)).url) {
            placeholder(R.drawable.stub)
            target(object : ImageViewTarget(thumbImage) {
                override fun onSuccess(result: Drawable) {
                    super.onSuccess(result)
                    thumbImage.isChecked = checked
                }
            })
        }
    }

    fun setOnClickMenu(listener: View.OnClickListener) {
        menu.tag = bindingAdapterPosition
        menu.setOnClickListener(listener)
    }

    fun setOnClickTags(listener: View.OnClickListener) {
        tagList.tag = bindingAdapterPosition
        tagList.setOnTagClickListener(listener)
    }

    fun setOnClickThumbnail(
        listener: View.OnClickListener,
        longClickListener: View.OnLongClickListener
    ) {
        itemView.tag = bindingAdapterPosition
        itemView.setOnClickListener(listener)
        itemView.setOnLongClickListener(longClickListener)
        itemView.isLongClickable = true
    }
}