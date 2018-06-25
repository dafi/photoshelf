package com.ternaryop.photoshelf.adapter

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
import com.ternaryop.photoshelf.util.network.ApiManager
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.htmlHighlightPattern

class TagAdapter(context: Context, val resource: Int, var blogName: String) : ArrayAdapter<TagInfo>(context, 0) {
    private var items = emptyList<TagInfo>()
    private val tagFilter = TagFilter(context, blogName)

    override fun getCount(): Int = items.size
    override fun getItem(position: Int): TagInfo = items[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getFilter(): Filter = tagFilter

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflatedView = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val item = getItem(position)
        (inflatedView as TextView).text = when {
            tagFilter.pattern == null || tagFilter.pattern!!.isEmpty() ->  context.getString(
                R.string.tag_with_post_count,
                item.tag,
                item.postCount)
            else -> {
                val htmlHighlightPattern = item.tag.htmlHighlightPattern(tagFilter.pattern!!.toString())
                context.getString(R.string.tag_with_post_count, htmlHighlightPattern, item.postCount).fromHtml()
            }
        }
        return inflatedView
    }

    inner class TagFilter(val context: Context, val blogName: String) : Filter() {
        var pattern: CharSequence? = null
        var error: Throwable? = null

        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as TagInfo).tag
        }

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val filterResults = FilterResults()
            filterResults.values = null
            filterResults.count = 0

            pattern = constraint?.trim() ?: ""

            try {
                error = null
                val list = ApiManager.postManager(context).findTags(pattern.toString(), blogName)
                filterResults.values = list
                filterResults.count = list.size
            } catch (e: Exception) {
                error = e
            }

            return filterResults
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults?) {
            error?.let {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }
            filterResults?.values?.let {
                items = it as List<TagInfo>

                notifyDataSetChanged()
            }
        }
    }
}