package com.ternaryop.photoshelf.adapter.photo

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.ternaryop.lazyimageloader.ImageLoader
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_CAPTION_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_MENU_OVERFLOW_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TIME_DESC_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TITLE_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_VIEW_BACKGROUND
import com.ternaryop.photoshelf.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.text.fromHtml
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.utils.StringUtils
import org.joda.time.format.DateTimeFormat

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

    fun bindModel(post: PhotoShelfPost, imageLoader: ImageLoader, thumbnailWidth: Int, showUploadTime: Boolean) {
        this.post = post
        updateTitles(showUploadTime)
        displayImage(imageLoader, thumbnailWidth)
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

    private fun displayImage(imageLoader: ImageLoader, thumbnailWidth: Int) {
        val altSize = post.getClosestPhotoByWidth(thumbnailWidth) ?: return
        setImageDimension(altSize, thumbnailWidth)

        imageLoader.displayImage(altSize.url, thumbImage)
    }

    private fun setImageDimension(altSize: TumblrAltSize, thumbnailWidth: Int) {
        val minThumbnailWidth = Math.max(thumbnailWidth, altSize.width)
        // convert from pixel to DIP
        with (thumbImage.layoutParams) {
            width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minThumbnailWidth.toFloat(), itemView.context.resources.displayMetrics).toInt()
            height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, altSize.height.toFloat(), itemView.context.resources.displayMetrics).toInt()
        }
    }

    private fun updateTitles(showUploadTime: Boolean) {
        caption.text = StringUtils.stripHtmlTags("a|img|p|br", post.caption).fromHtml()
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
        noteCountText.text = itemView.resources.getString(R.string.uploaded_at_time, DateTimeFormat.forPattern("dd/MM/yyyy HH:mm").print(post.timestamp * SECOND_IN_MILLIS))
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
            val position = adapterPosition
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
            itemView.tag = adapterPosition
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
        tagsContainer.tag = adapterPosition
        for (i in 0 until tagsContainer.childCount) {
            tagsContainer.getChildAt(i).setOnClickListener(listener)
        }
    }
}
