package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ternaryop.photoshelf.adapter.*
import com.ternaryop.photoshelf.tumblr.ui.core.R
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
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
    private val tagsContainer: ViewGroup = itemView.findViewById(R.id.tags_container)
    private lateinit var post: PhotoShelfPost

    fun bindModel(post: PhotoShelfPost, thumbnailWidth: Int, showUploadTime: Boolean) {
        this.post = post
        updateTitles(showUploadTime)
        displayImage(thumbnailWidth)
        setupTags()
        updateItemColors()
    }

    @SuppressLint("InflateParams")
    private fun setupTags() {
        val tags = post.tags
        val tagsCount = tags.size
        val viewCount = tagsContainer.childCount
        val delta = tagsCount - viewCount

        if (delta < 0) {
            for (i in tagsCount until viewCount) {
                tagsContainer.getChildAt(i).visibility = View.GONE
            }
        } else if (delta > 0) {
            for (i in 0 until delta) {
                tagsContainer.addView(LayoutInflater.from(tagsContainer.context).inflate(R.layout.other_tag, null))
            }
        }
        for (i in tags.indices) {
            val tag = tags[i]
            val view = tagsContainer.getChildAt(i) as TextView
            view.id = R.id.tag_text_view
            view.text = String.format("#%s", tag)
            view.tag = tag
            view.visibility = View.VISIBLE
        }
    }

    private fun setColors(resArray: Int) {
        val array = itemView.context.resources.obtainTypedArray(resArray)
        itemView.background = array.getDrawable(POST_STYLE_INDEX_VIEW_BACKGROUND)
        timeDesc.setTextColor(array.getColorStateList(POST_STYLE_INDEX_TIME_DESC_TEXT_COLOR))
        caption.setTextColor(array.getColorStateList(POST_STYLE_INDEX_CAPTION_TEXT_COLOR))
        menu.imageTintList = array.getColorStateList(POST_STYLE_INDEX_MENU_OVERFLOW_COLOR)
        noteCountText.setTextColor(array.getColorStateList(POST_STYLE_INDEX_CAPTION_TEXT_COLOR))

        val titleTextColor = array.getColorStateList(POST_STYLE_INDEX_TITLE_TEXT_COLOR)
        for (i in 0 until tagsContainer.childCount) {
            val view = tagsContainer.getChildAt(i) as TextView
            view.setTextColor(titleTextColor)
        }

        array.recycle()
    }

    private fun displayImage(thumbnailWidth: Int) {
        val altSize = post.getClosestPhotoByWidth(thumbnailWidth) ?: return
        setImageDimension(altSize, thumbnailWidth)

        thumbImage.load(altSize.url) {
            placeholder(R.drawable.stub)
        }
    }

    private fun setImageDimension(altSize: TumblrAltSize, thumbnailWidth: Int) {
        val minThumbnailWidth = max(thumbnailWidth, altSize.width)
        // convert from pixel to DIP
        thumbImage.layoutParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            minThumbnailWidth.toFloat(), itemView.context.resources.displayMetrics).toInt()
        thumbImage.layoutParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            altSize.height.toFloat(), itemView.context.resources.displayMetrics).toInt()
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
        noteCountText.text = itemView.resources.getString(R.string.uploaded_at_time,
            DATE_FORMATTER_FULL.format(post.timestamp.secondsToLocalDateTime()))
    }

    private fun updateNote() {
        val noteCount = post.noteCount.toInt()
        if (noteCount > 0) {
            noteCountText.visibility = View.VISIBLE
            noteCountText.text = itemView.context.resources.getQuantityString(
                    R.plurals.note_title,
                    noteCount,
                    noteCount)
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
            PhotoShelfPost.ScheduleTime.POST_PUBLISH_NEVER -> setColors(R.array.post_never)
            PhotoShelfPost.ScheduleTime.POST_PUBLISH_FUTURE -> setColors(R.array.post_future)
            else -> setColors(if (post.groupId % 2 == 0) R.array.post_even else R.array.post_odd)
        }
    }

    private fun setTagsClickListener(listener: View.OnClickListener) {
        tagsContainer.tag = bindingAdapterPosition
        for (i in 0 until tagsContainer.childCount) {
            tagsContainer.getChildAt(i).setOnClickListener(listener)
        }
    }
}
