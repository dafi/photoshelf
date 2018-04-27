package com.ternaryop.photoshelf.adapter.feedly

import android.os.Build
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso.Picasso
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
    val tag: TextView = itemView.findViewById(R.id.tag)

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

        content.domain?.let { domain ->
            Picasso
                .get()
                .load("https://www.google.com/s2/favicons?domain_url=$domain")
                .noFade()
                .placeholder(R.drawable.stub)
                .into(faviconImage)
        }
    }

    private fun setImageDimension(size: Int) {
        // convert from pixel to DIP
        faviconImage.layoutParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            size.toFloat(), itemView.context.resources.displayMetrics).toInt()
        faviconImage.layoutParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            size.toFloat(), itemView.context.resources.displayMetrics).toInt()
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

    private fun updateTag(content: FeedlyContentDelegate) {
        if (content.tag == null) {
            tag.visibility = GONE
        } else {
            tag.visibility = VISIBLE
            tag.text = String.format("#%s", content.tag)
        }
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

    fun setOnClickListeners(listener: View.OnClickListener?) {
        listener ?: return

        val position = adapterPosition
        itemView.setOnClickListener(listener)
        itemView.tag = position

        tag.setOnClickListener(listener)
        tag.tag = position
    }

    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener?) {
        listener ?: return

        val position = adapterPosition
        checkbox.setOnCheckedChangeListener(listener)
        checkbox.tag = position
    }
}
