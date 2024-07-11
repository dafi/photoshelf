package com.ternaryop.photoshelf.feedly.adapter

import android.util.TypedValue
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ternaryop.photoshelf.feedly.R
import java.util.Locale

private const val FAVICON_SIZE = 16
private const val LIST_ITEM_STYLE_INDEX_VIEW_BACKGROUND = 0
private const val LIST_ITEM_STYLE_INDEX_TITLE = 1
private const val LIST_ITEM_STYLE_INDEX_SUBTITLE = 2
private const val LIST_ITEM_STYLE_INDEX_TAG = 3

/**
 * Created by dave on 24/02/17.
 * The ViewHolder used by the Feedly list
 */
@Suppress("MemberVisibilityCanBePrivate")
class FeedlyContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(android.R.id.text1)
    val subtitle: TextView = itemView.findViewById(android.R.id.text2)
    val checkbox: CheckBox = itemView.findViewById(android.R.id.checkbox)
    val faviconImage: ImageView = itemView.findViewById(com.ternaryop.photoshelf.core.R.id.thumbnail_image)
    val sidebar: View = itemView.findViewById(com.ternaryop.photoshelf.core.R.id.sidebar)
    val tag: TextView = itemView.findViewById(com.ternaryop.photoshelf.core.R.id.tag)

    fun bindModel(content: FeedlyContentDelegate) {
        // setting listener to null resolved the lost of unchecked state
        // http://stackoverflow.com/a/32428115/195893
        checkbox.setOnCheckedChangeListener(null)
        updateCheckbox(content)
        updateTitles(content)
        updateItemColors(content)
        updateTag(content)
        displayImage(content, FAVICON_SIZE)
    }

    private fun displayImage(content: FeedlyContentDelegate, size: Int) {
        setImageDimension(size)

        val url = content.domain?.let { "https://www.google.com/s2/favicons?domain_url=$it" } ?: com.ternaryop.photoshelf.core.R.drawable.stub
        faviconImage.load(url) {
            placeholder(com.ternaryop.photoshelf.core.R.drawable.stub)
            error(com.ternaryop.photoshelf.core.R.drawable.stat_notify_error)
        }
    }

    private fun setImageDimension(size: Int) {
        // convert from pixel to DIP
        faviconImage.layoutParams.width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), itemView.context.resources.displayMetrics
        ).toInt()
        faviconImage.layoutParams.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), itemView.context.resources.displayMetrics
        ).toInt()
    }

    private fun updateCheckbox(content: FeedlyContentDelegate) {
        sidebar.visibility = VISIBLE
        checkbox.setButtonDrawable(R.drawable.checkbox_bookmark)
        checkbox.isChecked = content.isChecked
    }

    private fun updateTitles(content: FeedlyContentDelegate) {
        title.text = content.title

        if (content.actionTimestamp == 0L) {
            subtitle.text = String.format(
                Locale.US,
                "%s / %s", content.origin.title,
                content.getLastPublishTimestampAsString(itemView.context)
            )
        } else {
            subtitle.text = String.format(
                Locale.US,
                "%s / %s / %s", content.origin.title,
                content.getActionTimestampAsString(itemView.context),
                content.getLastPublishTimestampAsString(itemView.context)
            )
        }
    }

    private fun updateTag(content: FeedlyContentDelegate) {
        if (content.tag == null) {
            tag.visibility = GONE
        } else {
            tag.visibility = VISIBLE
            tag.text = String.format(Locale.US, "#%s", content.tag)
        }
    }

    private fun updateItemColors(content: FeedlyContentDelegate) {
        if (content.lastPublishTimestamp < 0) {
            setAppearance(R.array.feedly_item_never_published_array, content.isChecked)
        } else {
            setAppearance(R.array.feedly_item_published_array, content.isChecked)
        }
    }

    private fun setAppearance(resArray: Int, checked: Boolean) {
        title.isSelected = checked
        subtitle.isSelected = checked
        tag.isSelected = checked

        val array = itemView.context.resources.obtainTypedArray(resArray)
        itemView.background = array.getDrawable(LIST_ITEM_STYLE_INDEX_VIEW_BACKGROUND)

        TextViewCompat.setTextAppearance(title, array.getResourceId(LIST_ITEM_STYLE_INDEX_TITLE, 0))
        TextViewCompat.setTextAppearance(subtitle, array.getResourceId(LIST_ITEM_STYLE_INDEX_SUBTITLE, 0))
        TextViewCompat.setTextAppearance(tag, array.getResourceId(LIST_ITEM_STYLE_INDEX_TAG, 0))
        array.recycle()
    }

    fun setOnClickListeners(content: FeedlyContentDelegate, listener: View.OnClickListener?) {
        listener ?: return

        itemView.setOnClickListener(listener)
        itemView.tag = content.id

        tag.setOnClickListener(listener)
        tag.tag = content.id
    }

    fun setOnCheckedChangeListener(content: FeedlyContentDelegate, listener: CompoundButton.OnCheckedChangeListener?) {
        listener ?: return

        checkbox.setOnCheckedChangeListener(listener)
        checkbox.tag = content.id
    }
}
