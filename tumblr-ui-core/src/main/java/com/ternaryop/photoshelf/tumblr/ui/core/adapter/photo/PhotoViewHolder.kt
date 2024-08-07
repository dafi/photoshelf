package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_CAPTION_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_MENU_OVERFLOW_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TIME_DESC_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TITLE_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_VIEW_BACKGROUND
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.widget.TagListLayout
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.utils.date.secondsToLocalDateTime
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.stripHtmlTags
import java.time.format.DateTimeFormatter
import kotlin.math.max

private val DATE_FORMATTER_FULL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * Created by dave on 13/04/16.
 * The ViewHolder used by Photo objects
 */
class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val timeDesc: TextView = itemView.findViewById(R.id.time_desc)
    private val caption: TextView = itemView.findViewById(R.id.caption)
    private val thumbImage: ImageView = itemView.findViewById(R.id.thumbnail_image)
    private val menu: ImageView = itemView.findViewById(R.id.menu)
    private val noteCountText: TextView = itemView.findViewById(R.id.note_count)
    private val tagList: TagListLayout = itemView.findViewById(R.id.tags_container)
    private lateinit var post: PhotoShelfPost

    fun bindModel(post: PhotoShelfPost, thumbnailWidth: Int, showUploadTime: Boolean) {
        this.post = post
        updateTitles(showUploadTime)
        displayImage(thumbnailWidth)
        tagList.addTags(post.tags)
        updateItemColors()
    }

    private fun setColors(resArray: Int) {
        val array = itemView.context.resources.obtainTypedArray(resArray)
        itemView.background = array.getDrawable(POST_STYLE_INDEX_VIEW_BACKGROUND)
        timeDesc.setTextColor(array.getColorStateList(POST_STYLE_INDEX_TIME_DESC_TEXT_COLOR))
        caption.setTextColor(array.getColorStateList(POST_STYLE_INDEX_CAPTION_TEXT_COLOR))
        menu.imageTintList = array.getColorStateList(POST_STYLE_INDEX_MENU_OVERFLOW_COLOR)
        noteCountText.setTextColor(array.getColorStateList(POST_STYLE_INDEX_CAPTION_TEXT_COLOR))

        tagList.setTagTextColor(array.getColorStateList(POST_STYLE_INDEX_TITLE_TEXT_COLOR))

        array.recycle()
    }

    private fun displayImage(thumbnailWidth: Int) {
        val altSize = post.getClosestPhotoByWidth(thumbnailWidth) ?: return
        setImageDimension(altSize, thumbnailWidth)

        thumbImage.load(altSize.url) {
            placeholder(com.ternaryop.photoshelf.core.R.drawable.stub)
        }
    }

    private fun setImageDimension(altSize: TumblrAltSize, thumbnailWidth: Int) {
        val minThumbnailWidth = max(thumbnailWidth, altSize.width)
        // convert from pixel to DIP
        thumbImage.layoutParams.width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            minThumbnailWidth.toFloat(),
            itemView.context.resources.displayMetrics
        ).toInt()
        thumbImage.layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            altSize.height.toFloat(),
            itemView.context.resources.displayMetrics
        ).toInt()
    }

    private fun updateTitles(showUploadTime: Boolean) {
        caption.text = post.caption.stripHtmlTags("a|img|p|br").fromHtml()
        timeDesc.text = post.lastPublishedTimestampAsString
        // use noteCountText for both uploadTime and notes
        if (showUploadTime) {
            showUploadTime()
        } else {
            updateNote()
        }
    }

    private fun showUploadTime() {
        noteCountText.visibility = View.VISIBLE
        noteCountText.text = itemView.resources.getString(
            R.string.uploaded_at_time,
            DATE_FORMATTER_FULL.format(post.timestamp.secondsToLocalDateTime())
        )
    }

    private fun updateNote() {
        val noteCount = post.noteCount.toInt()
        if (noteCount > 0) {
            noteCountText.visibility = View.VISIBLE
            noteCountText.text = itemView.context.resources.getQuantityString(
                R.plurals.note_title,
                noteCount,
                noteCount
            )
        } else {
            noteCountText.visibility = View.GONE
        }
    }

    fun setOnClickListeners(listener: View.OnClickListener?) {
        if (listener != null) {
            setTagsClickListener(listener)
            val position = bindingAdapterPosition
            thumbImage.setOnClickListener(listener)
            thumbImage.tag = position

            menu.setOnClickListener(listener)
            menu.tag = position
        }
    }

    fun setOnClickMultiChoiceListeners(listener: View.OnClickListener?, longClickListener: View.OnLongClickListener) {
        if (listener != null) {
            itemView.setOnClickListener(listener)
            itemView.setOnLongClickListener(longClickListener)
            itemView.isLongClickable = true
            itemView.tag = bindingAdapterPosition
        }
    }

    private fun updateItemColors() {
        when (post.scheduleTimeType) {
            PhotoShelfPost.ScheduleTime.POST_PUBLISH_NEVER ->
                setColors(com.ternaryop.photoshelf.core.R.array.post_never)
            PhotoShelfPost.ScheduleTime.POST_PUBLISH_FUTURE ->
                setColors(com.ternaryop.photoshelf.core.R.array.post_future)
            else ->
                setColors(
                    if (post.groupId % 2 == 0) {
                        com.ternaryop.photoshelf.core.R.array.post_even
                    } else {
                        com.ternaryop.photoshelf.core.R.array.post_odd
                    }
                )
        }
    }

    private fun setTagsClickListener(listener: View.OnClickListener) {
        tagList.tag = bindingAdapterPosition
        tagList.setOnTagClickListener(listener)
    }
}
