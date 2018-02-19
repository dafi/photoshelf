package com.ternaryop.photoshelf.adapter.feedly

import android.os.Build
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.ternaryop.feedly.FeedlyContent
import com.ternaryop.lazyimageloader.ImageLoader
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TITLE_STYLE
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_TITLE_TEXT_COLOR
import com.ternaryop.photoshelf.adapter.POST_STYLE_INDEX_VIEW_BACKGROUND

/**
 * Created by dave on 24/02/17.
 * The ViewHolder used by the Feedly list
 */
private const val FAVICON_SIZE = 16

@Suppress("MemberVisibilityCanBePrivate")
class FeedlyContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(android.R.id.text1)
    val subtitle: TextView = itemView.findViewById(android.R.id.text2)
    val checkbox: CheckBox = itemView.findViewById(android.R.id.checkbox)
    val faviconImage: ImageView = itemView.findViewById(R.id.thumbnail_image)
    val sidebar: View = itemView.findViewById(R.id.sidebar)

    fun bindModel(content: FeedlyContentDelegate, imageLoader: ImageLoader) {
        // setting listener to null resolved the lost of unchecked state
        // http://stackoverflow.com/a/32428115/195893
        checkbox.setOnCheckedChangeListener(null)
        updateCheckbox(content)
        updateTitles(content)
        updateItemColors(content)
        displayImage(content, imageLoader, FAVICON_SIZE)
    }

    private fun displayImage(content: FeedlyContentDelegate, imageLoader: ImageLoader, size: Int) {
        setImageDimension(size)

        if (content.domain != null) {
            imageLoader.displayImage("https://www.google.com/s2/favicons?domain_url=${content.domain}", faviconImage)
        }
    }

    private fun setImageDimension(size: Int) {
        with (faviconImage.layoutParams) {
            // convert from pixel to DIP
            width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), itemView.context.resources.displayMetrics).toInt()
            height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), itemView.context.resources.displayMetrics).toInt()
        }
    }

    private fun updateCheckbox(content: FeedlyContentDelegate) {
        sidebar.visibility = View.VISIBLE
        checkbox.setButtonDrawable(R.drawable.checkbox_bookmark)
        checkbox.isChecked = content.isChecked
    }

    private fun updateTitles(content: FeedlyContentDelegate) {
        title.text = content.title

        subtitle.text = String.format("%s / %s / %s", content.origin.title,
                content.getActionTimestampAsString(itemView.context),
                content.getLastPublishTimestampAsString(itemView.context))
    }

    private fun updateItemColors(content: FeedlyContentDelegate) {
        if (content.lastPublishTimestamp < 0) {
            setColors(R.array.post_never)
        } else {
            setColors(R.array.post_normal)
        }
    }

    @Suppress("MagicNumber")
    private fun setColors(resArray: Int) {
        val array = itemView.context.resources.obtainTypedArray(resArray)
        itemView.background = array.getDrawable(POST_STYLE_INDEX_VIEW_BACKGROUND)

        val titleStyle = array.getResourceId(POST_STYLE_INDEX_TITLE_STYLE, 0)
        if (Build.VERSION.SDK_INT < 23) {
            title.setTextAppearance(itemView.context, titleStyle)
        } else {
            title.setTextAppearance(titleStyle)
        }
        subtitle.setTextColor(array.getColorStateList(POST_STYLE_INDEX_TITLE_TEXT_COLOR))
        array.recycle()
    }

    fun setOnClickListeners(content: FeedlyContent, listener: View.OnClickListener?) {
        if (listener == null) {
            return
        }
        val position = adapterPosition
        itemView.setOnClickListener(listener)
        itemView.tag = position
    }

    fun setOnCheckedChangeListener(content: FeedlyContentDelegate, listener: CompoundButton.OnCheckedChangeListener?) {
        if (listener == null) {
            return
        }
        val position = adapterPosition
        checkbox.setOnCheckedChangeListener(listener)
        checkbox.tag = position
    }
}
