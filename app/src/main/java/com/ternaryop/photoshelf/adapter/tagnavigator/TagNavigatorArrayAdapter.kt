package com.ternaryop.photoshelf.adapter.tagnavigator

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.TextView
import android.widget.Toast
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.post.TagInfo
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.htmlHighlightPattern

/**
 * Used with spinners because the RecyclerView version isn't compatible with ListView
 */
class TagNavigatorArrayAdapter(context: Context, val resource: Int, var blogName: String) :
    ArrayAdapter<TagInfo>(context, 0),
    TagNavigatorFilterListener {
    private val items = mutableListOf<TagInfo>()
    private val tagFilter = TagNavigatorFilter(context, blogName, this)

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): TagInfo = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = tagFilter

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflatedView = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val tagInfo = getItem(position)

        // Click on ListView items doesn't work if the following attributes are set to true, so we turn them off
        inflatedView.isClickable = false
        inflatedView.isFocusable = false

        val tagView: TextView = inflatedView.findViewById(R.id.tag)
        val countView: TextView = inflatedView.findViewById(R.id.count)
        val pattern = tagFilter.pattern

        tagView.text = if (pattern == null || pattern.isBlank()) {
            tagInfo.tag
        } else {
            tagInfo.tag.htmlHighlightPattern(pattern.toString()).fromHtml()
        }
        countView.text = String.format("%3d", tagInfo.postCount)
        return inflatedView
    }

    override fun onError(filter: TagNavigatorFilter, throwable: Throwable) {
        Toast.makeText(context, throwable.message, Toast.LENGTH_LONG).show()
    }

    override fun onComplete(filter: TagNavigatorFilter, items: List<TagInfo>) {
        this.items.clear()
        this.items.addAll(items)

        notifyDataSetChanged()
    }
}
