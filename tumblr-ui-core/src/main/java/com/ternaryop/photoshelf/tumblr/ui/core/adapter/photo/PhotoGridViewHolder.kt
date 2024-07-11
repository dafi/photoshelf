package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_MENU_OVERFLOW_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TIME_DESC_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TITLE_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_VIEW_BACKGROUND
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.widget.TagListLayout
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.widget.CheckableImageView

class PhotoGridViewHolder(vi: View) : RecyclerView.ViewHolder(vi) {
    private val tagList: TagListLayout = itemView.findViewById(R.id.tags_container)
    private val thumbImage = vi.findViewById<View>(R.id.thumbnail_image) as CheckableImageView
    private val menu = vi.findViewById<View>(R.id.menu) as ImageView
    private val timeDesc: TextView = itemView.findViewById(R.id.time_desc)
    private lateinit var post: PhotoShelfPost

    fun bindModel(
        post: PhotoShelfPost,
        checked: Boolean,
        paintGridCellByScheduleTimeType: Boolean
    ) {
        this.post = post
        displayImage(checked)
        updateTexts()
        updateItemColors(paintGridCellByScheduleTimeType)
    }

    private fun setColors(resArray: Int) {
        val array = itemView.context.resources.obtainTypedArray(resArray)
        itemView.background = array.getDrawable(POST_STYLE_INDEX_VIEW_BACKGROUND)
        timeDesc.setTextColor(array.getColorStateList(POST_STYLE_INDEX_TIME_DESC_TEXT_COLOR))
        menu.imageTintList = array.getColorStateList(POST_STYLE_INDEX_MENU_OVERFLOW_COLOR)

        tagList.setTagTextColor(array.getColorStateList(POST_STYLE_INDEX_TITLE_TEXT_COLOR))

        array.recycle()
    }

    private fun updateItemColors(colorCellByScheduleTimeType: Boolean) {
        if (colorCellByScheduleTimeType) {
            when (post.scheduleTimeType) {
                PhotoShelfPost.ScheduleTime.POST_PUBLISH_NEVER -> setColors(com.ternaryop.photoshelf.core.R.array.post_never)
                PhotoShelfPost.ScheduleTime.POST_PUBLISH_FUTURE -> setColors(com.ternaryop.photoshelf.core.R.array.post_future)
                else -> setColors(com.ternaryop.photoshelf.core.R.array.post_normal)
            }
        } else {
            setColors(com.ternaryop.photoshelf.core.R.array.post_normal)
        }
    }

    private fun updateTexts() {
        tagList.addTags(post.tags)
        timeDesc.text = post.lastPublishedTimestampAsString
    }

    private fun displayImage(checked: Boolean) {
        thumbImage.load(checkNotNull(post.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_250)).url) {
            placeholder(com.ternaryop.photoshelf.core.R.drawable.stub)
            listener(
                onSuccess = { _, _ -> thumbImage.isChecked = checked }
            )
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
        thumbImage.tag = bindingAdapterPosition
        thumbImage.setOnClickListener(listener)
        thumbImage.setOnLongClickListener(longClickListener)
        thumbImage.isLongClickable = true
    }
}
