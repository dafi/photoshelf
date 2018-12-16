package com.ternaryop.photoshelf.adapter.tagnavigator

import android.content.Context
import android.widget.Filter
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.TagInfo

interface TagNavigatorFilterListener {
    fun onComplete(filter: TagNavigatorFilter, items: List<TagInfo>)
    fun onError(filter: TagNavigatorFilter, throwable: Throwable)
}

class TagNavigatorFilter(
    context: Context,
    var blogName: String,
    val listener: TagNavigatorFilterListener) : Filter() {
    val postService = ApiManager.postService(context)
    var pattern: CharSequence? = null
    var error: Throwable? = null

    override fun convertResultToString(resultValue: Any?): CharSequence {
        return (resultValue as? TagInfo)?.tag ?: ""
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val filterResults = FilterResults()
        filterResults.values = null
        filterResults.count = 0

        pattern = constraint?.trim() ?: ""

        error = null
        try {
            val response = postService
                .findTags(blogName, pattern.toString())
                .blockingGet()
            filterResults.values = response.response.tags
            filterResults.count = response.response.tags.size
        } catch (t: Throwable) {
            error = t
        }

        return filterResults
    }

    @Suppress("UNCHECKED_CAST")
    override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults?) {
        error?.let { listener.onError(this, it) }
        filterResults?.values?.let { listener.onComplete(this, it as List<TagInfo>) }
    }
}