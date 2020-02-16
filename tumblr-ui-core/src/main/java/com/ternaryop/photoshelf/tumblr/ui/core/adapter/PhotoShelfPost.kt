package com.ternaryop.photoshelf.tumblr.ui.core.adapter

import android.text.format.DateUtils.SECOND_IN_MILLIS
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.utils.date.APPEND_DATE_FOR_PAST_AND_PRESENT
import com.ternaryop.utils.date.formatPublishDaysAgo

/**
 * The last published time can be in the future if the post is scheduled
 */
class PhotoShelfPost(photoPost: TumblrPhotoPost, var lastPublishedTimestamp: Long) : TumblrPhotoPost(photoPost) {
    var groupId: Int = 0

    val scheduleTimeType: ScheduleTime
        get() = when {
            lastPublishedTimestamp == java.lang.Long.MAX_VALUE -> ScheduleTime.POST_PUBLISH_NEVER
            lastPublishedTimestamp > System.currentTimeMillis() -> ScheduleTime.POST_PUBLISH_FUTURE
            else -> ScheduleTime.POST_PUBLISH_PAST
        }

    val lastPublishedTimestampAsString: String
        get() {
            val tt = if (scheduledPublishTime > 0) scheduledPublishTime * SECOND_IN_MILLIS else lastPublishedTimestamp
            return tt.formatPublishDaysAgo(APPEND_DATE_FOR_PAST_AND_PRESENT)
        }

    enum class ScheduleTime {
        POST_PUBLISH_NEVER,
        POST_PUBLISH_FUTURE,
        POST_PUBLISH_PAST
    }

    companion object {
        private const val serialVersionUID = -6670033021694674250L
    }
}
