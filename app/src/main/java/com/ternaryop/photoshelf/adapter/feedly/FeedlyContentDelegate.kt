package com.ternaryop.photoshelf.adapter.feedly

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateUtils.SECOND_IN_MILLIS
import com.ternaryop.feedly.FeedlyContent
import com.ternaryop.photoshelf.R
import com.ternaryop.utils.DateTimeUtils
import java.net.URI
import java.net.URISyntaxException

/**
 * Contains fields related to UI state
 */
class FeedlyContentDelegate(private val delegated: FeedlyContent) : FeedlyContent by delegated {
    var isChecked = true
    var lastPublishTimestamp: Long = 0
    var domain: String? = null
        private set

    init {
        try {
            domain = URI(originId).host
        } catch (ignored: URISyntaxException) {
        }
    }

    fun getLastPublishTimestampAsString(context: Context): String {
        return if (lastPublishTimestamp <= 0) {
            context.getString(R.string.never_published)
        } else DateTimeUtils.formatPublishDaysAgo(lastPublishTimestamp * SECOND_IN_MILLIS, DateTimeUtils.APPEND_DATE_FOR_PAST_AND_PRESENT)
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
