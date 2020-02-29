package com.ternaryop.photoshelf.imagepicker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.util.notification.EXTRA_NOTIFICATION_TAG
import com.ternaryop.photoshelf.util.notification.notificationManager

internal const val EXTRA_URI = "com.ternaryop.photoshelf.imagepicker.extra.URI"
internal const val EXTRA_POST_TITLE = "com.ternaryop.photoshelf.imagepicker.extra.POST_TITLE"
internal const val EXTRA_POST_TAGS = "com.ternaryop.photoshelf.imagepicker.extra.POST_TAGS"
internal const val EXTRA_ACTION = "com.ternaryop.photoshelf.imagepicker.extra.ACTION"
internal const val EXTRA_PUBLISH_CLASS_NAME = "com.ternaryop.photoshelf.imagepicker.extra.PUBLISH_CLASS_NAME"

fun Intent.toPostPublisherData() = PostPublisherData(
    checkNotNull(getStringExtra(EXTRA_URI)),
    checkNotNull(getStringExtra(EXTRA_BLOG_NAME)),
    checkNotNull(getStringExtra(EXTRA_POST_TITLE)),
    checkNotNull(getStringExtra(EXTRA_POST_TAGS)),
    checkNotNull(PostPublisherAction.fromInt(getIntExtra(EXTRA_ACTION, Integer.MIN_VALUE))),
    getStringExtra(EXTRA_PUBLISH_CLASS_NAME)
)

class RetryPublishNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = intent.toPostPublisherData()
        val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)
        context.notificationManager.cancel(notificationTag, data.url.hashCode())

        PostPublisherService.startPublish(context, data)
    }

    companion object {
        fun createIntent(
            context: Context,
            data: PostPublisherData
        ) = Intent(context, RetryPublishNotificationBroadcastReceiver::class.java)
            .putExtra(EXTRA_ACTION, data.action.ordinal)
            .putExtra(EXTRA_URI, data.url)
            .putExtra(EXTRA_BLOG_NAME, data.blogName)
            .putExtra(EXTRA_POST_TITLE, data.postTitle)
            .putExtra(EXTRA_POST_TAGS, data.postTags)
            .putExtra(EXTRA_PUBLISH_CLASS_NAME, data.publishClassName)
    }
}
