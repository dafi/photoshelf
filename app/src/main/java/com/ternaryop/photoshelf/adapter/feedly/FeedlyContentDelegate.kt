package com.ternaryop.photoshelf.adapter.feedly

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.SECOND_IN_MILLIS
import com.ternaryop.feedly.FeedlyContent
import com.ternaryop.photoshelf.R
import com.ternaryop.utils.date.APPEND_DATE_FOR_PAST_AND_PRESENT
import com.ternaryop.utils.date.formatPublishDaysAgo
import java.net.URI
import java.net.URISyntaxException

/**
 * Convert Collection<FeedlyContent> to List<FeedlyContentDelegate>
 */
fun Collection<FeedlyContent>.toContentDelegate() = map { FeedlyContentDelegate(it) }

fun Collection<FeedlyContentDelegate>.titles(): Collection<String> {
    val titles = HashSet<String>(size)
    for (fc in this) {
        // replace any no no-break space with whitespace
        // see http://www.regular-expressions.info/unicode.html for \p{Zs}
        titles.add(fc.title.replace("""\p{Zs}""".toRegex(), " "))
    }
    return titles
}

/**
 * update lastPublishTimestamp and the tag fields
 */
fun Collection<FeedlyContentDelegate>.updateLastPublishTimestamp(tagPairList: Map<String, Long>): Collection<FeedlyContentDelegate> {
    for (fc in this) {
        updateLastPublishTimestamp(fc, tagPairList)
    }
    return this
}

private fun updateLastPublishTimestamp(fc: FeedlyContentDelegate, tagPairList: Map<String, Long>) {
    val title = fc.title
    for ((tag, time) in tagPairList) {
        if (tag.regionMatches(0, title, 0, tag.length, ignoreCase = true)) {
            fc.lastPublishTimestamp = time
            fc.tag = tag
        }
    }
}

/**
 * Contains fields related to UI state
 */
class FeedlyContentDelegate(private val delegated: FeedlyContent) : FeedlyContent by delegated {
    var isChecked = true
    var lastPublishTimestamp = Long.MIN_VALUE
    var domain: String? = null
        private set
    var tag: String? = null

    init {
        try {
            domain = URI(originId).host
        } catch (ignored: URISyntaxException) {
        }
    }

    fun getLastPublishTimestampAsString(context: Context): String {
        return if (lastPublishTimestamp <= 0) {
            context.getString(R.string.never_published)
        } else (lastPublishTimestamp * SECOND_IN_MILLIS).formatPublishDaysAgo(APPEND_DATE_FOR_PAST_AND_PRESENT)
    }

    fun getActionTimestampAsString(context: Context): String {
        return DateUtils.getRelativeTimeSpanString(context, actionTimestamp).toString()
    }

    fun compareTitle(other: FeedlyContent): Int = title.compareTo(other.title, ignoreCase = true)

    fun compareActionTimestamp(other: FeedlyContent): Int {
        val at1 = actionTimestamp
        val at2 = other.actionTimestamp
        return if (at1 == at2) 0 else if (at1 < at2) 1 else -1
    }

    fun compareLastTimestamp(other: FeedlyContentDelegate): Int {
        val t1 = lastPublishTimestamp
        val t2 = other.lastPublishTimestamp

        return if (t1 == t2) {
            compareTitle(other)
        } else {
            if (t1 < t2) -1 else 1
        }
    }
}
