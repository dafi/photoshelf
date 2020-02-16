package com.ternaryop.photoshelf.imagepicker.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.imagepicker.R
import com.ternaryop.photoshelf.service.AbsIntentService
import com.ternaryop.photoshelf.util.notification.EXTRA_NOTIFICATION_TAG
import com.ternaryop.photoshelf.util.notification.notificationManager
import com.ternaryop.photoshelf.util.notification.notify
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.publishPhotoPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.URI

const val EXTRA_URI = "com.ternaryop.photoshelf.extra.URI"

const val EXTRA_POST_TITLE = "com.ternaryop.photoshelf.extra.POST_TITLE"
const val EXTRA_POST_TAGS = "com.ternaryop.photoshelf.extra.POST_TAGS"

const val ON_PUBLISH_CLASS_NAME = "com.ternaryop.photoshelf.imagepicker.service.onPublishClassName"

/**
 * Created by dave on 01/03/14.
 * Contains all methods used to publish posts
 */
class PublishIntentService : AbsIntentService("publishIntent") {
    private var onPublishInstance: OnPublish? = null

    override fun onCreate() {
        super.onCreate()

        try {
            onPublishInstance = getOnPublishClassName()?.let { name -> Class.forName(name).newInstance() as OnPublish }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        intent ?: return

        try {
            onPublishInstance?.also { onPublish(it, intent) }
            when (intent.action) {
                ACTION_PUBLISH_DRAFT -> publish(intent, true)
                ACTION_PUBLISH_PUBLISH -> publish(intent, false)
            }
        } catch (e: Exception) {
            val url = intent.getParcelableExtra<Uri>(EXTRA_URI) ?: ""
            val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: ""
            e.notify(this, postTags, getString(R.string.upload_error_ticker), url.hashCode())
        }
    }

    private fun publish(intent: Intent, isDraft: Boolean) {
        val url = checkNotNull(intent.getParcelableExtra<Uri>(EXTRA_URI))
        val selectedBlogName = checkNotNull(intent.getStringExtra(EXTRA_BLOG_NAME))
        val postTitle = checkNotNull(intent.getStringExtra(EXTRA_POST_TITLE))
        val postTags = checkNotNull(intent.getStringExtra(EXTRA_POST_TAGS))

        try {
            if (isDraft) {
                TumblrManager.getInstance(applicationContext)
                    .draftPhotoPost(selectedBlogName, URI(url.toString()), postTitle, postTags)
            } else {
                TumblrManager.getInstance(applicationContext)
                    .publishPhotoPost(selectedBlogName, URI(url.toString()), postTitle, postTags)
            }
        } catch (e: Exception) {
            e.notify(this, postTags, getString(R.string.retry),
                createRetryPublishIntent(intent), url.hashCode())
        }
    }

    private fun onPublish(onPublishCallback: OnPublish, intent: Intent) {
        val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: return
        val tagList = TumblrPost.tagsFromString(postTags)

        runBlocking(Dispatchers.IO) {
            try {
                onPublishCallback.publish(applicationContext, tagList)
            } catch (t: Throwable) {
                t.notify(applicationContext, name, t.message)
            }
        }
    }

    private fun getOnPublishClassName(): String? {
        val componentName = ComponentName(this, javaClass)
        val data = packageManager.getServiceInfo(componentName, PackageManager.GET_META_DATA).metaData
        return data.getString(ON_PUBLISH_CLASS_NAME)
    }

    class RetryPublishNotificationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val url = checkNotNull(intent.getParcelableExtra<Uri>(EXTRA_URI))
            val selectedBlogName = checkNotNull(intent.getStringExtra(EXTRA_BLOG_NAME))
            val postTitle = checkNotNull(intent.getStringExtra(EXTRA_POST_TITLE))
            val postTags = checkNotNull(intent.getStringExtra(EXTRA_POST_TAGS))
            val action = intent.action
            val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)

            context.notificationManager.cancel(notificationTag, url.hashCode())

            startActionIntent(context,
                url,
                selectedBlogName,
                postTitle,
                postTags,
                action == ACTION_PUBLISH_PUBLISH)
        }
    }

    private fun createRetryPublishIntent(failedIntent: Intent): Intent {
        return Intent(this, RetryPublishNotificationBroadcastReceiver::class.java)
            .setAction(failedIntent.action)
            .putExtra(EXTRA_URI, failedIntent.getParcelableExtra<Uri>(EXTRA_URI))
            .putExtra(EXTRA_BLOG_NAME, failedIntent.getStringExtra(EXTRA_BLOG_NAME))
            .putExtra(EXTRA_POST_TITLE, failedIntent.getStringExtra(EXTRA_POST_TITLE))
            .putExtra(EXTRA_POST_TAGS, failedIntent.getStringExtra(EXTRA_POST_TAGS))
    }

    companion object {
        private const val ACTION_PUBLISH_DRAFT = "draft"
        private const val ACTION_PUBLISH_PUBLISH = "publish"

        @Suppress("LongParameterList")
        fun startActionIntent(
            context: Context,
            url: Uri,
            blogName: String,
            postTitle: String,
            postTags: String,
            publish: Boolean
        ) {
            val intent = Intent(context, PublishIntentService::class.java)
                .setAction(if (publish) ACTION_PUBLISH_PUBLISH else ACTION_PUBLISH_DRAFT)
                .putExtra(EXTRA_URI, url)
                .putExtra(EXTRA_BLOG_NAME, blogName)
                .putExtra(EXTRA_POST_TITLE, postTitle)
                .putExtra(EXTRA_POST_TAGS, postTags)

            context.startService(intent)
        }
    }
}
