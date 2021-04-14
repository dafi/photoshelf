package com.ternaryop.photoshelf.tumblr.ui.draft.fragment

import android.text.format.DateUtils
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.date.millisecond
import com.ternaryop.utils.date.second
import java.util.Calendar
import java.util.Date

internal class ScheduleDate(val defaultScheduleMinutesTimeSpan: Int) {
    var currentDate: Calendar? = null

    fun peekNext(posts: List<TumblrPost>): Calendar {
        val cal = currentDate?.clone() as? Calendar ?: getMostRecent(posts)

        // set next queued post time
        cal.add(Calendar.MINUTE, defaultScheduleMinutesTimeSpan)

        return cal
    }

    private fun getMostRecent(posts: List<TumblrPost>): Calendar {
        val maxScheduledTime = posts.maxByOrNull { it.scheduledPublishTime }
            ?.run { scheduledPublishTime * DateUtils.SECOND_IN_MILLIS } ?: System.currentTimeMillis()

        // Calendar.MINUTE isn't reset otherwise the calc may be inaccurate
        val cal = Calendar.getInstance()
        cal.time = Date(maxScheduledTime)
        cal.second = 0
        cal.millisecond = 0
        return cal
    }
}
