package com.ternaryop.photoshelf.tagnavigator.adapter

import android.widget.Filter
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.TagInfo
import kotlinx.coroutines.runBlocking

interface TagNavigatorFilterListener {
    fun onComplete(filter: TagNavigatorFilter, items: List<TagInfo>)
    fun onError(filter: TagNavigatorFilter, throwable: Throwable)
}

class TagNavigatorFilter(
    var blogName: String,
    val listener: TagNavigatorFilterListener
) : Filter() {
    val postService = ApiManager.postService()
    var pattern: CharSequence? = null
    var error: Throwable? = null

    override fun convertResultToString(resultValue: Any?): CharSequence {
        return (resultValue as? TagInfo)?.tag ?: ""
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        val filterResults = FilterResults()
        filterResults.values = null
        filterResults.count = 0

        error = null

        // execute findTags() only if constraint is not null
        pattern = constraint?.trim() ?: return filterResults

        try {
            runBlocking {
                val response = postService.findTags(blogName, pattern.toString())
                filterResults.values = response.response.tags
                filterResults.count = response.response.tags.size
            }
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
