package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_NOTIFICATION_TAG
import com.ternaryop.photoshelf.EXTRA_POST_TAGS
import com.ternaryop.photoshelf.EXTRA_POST_TITLE
import com.ternaryop.photoshelf.EXTRA_URI
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.publishPhotoPost
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.URI

/**
 * Created by dave on 01/03/14.
 * Contains all methods used to publish posts
 */
class PublishIntentService : IntentService("publishIntent") {

    private lateinit var notificationUtil: NotificationUtil

    override fun onCreate() {
        super.onCreate()
        notificationUtil = NotificationUtil(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        intent ?: return

        try {
            addBirthdateFromTags(intent)
            when (intent.action) {
                ACTION_PUBLISH_DRAFT -> publish(intent, true)
                ACTION_PUBLISH_PUBLISH -> publish(intent, false)
            }
        } catch (e: Exception) {
            val url = intent.getParcelableExtra<Uri>(EXTRA_URI) ?: ""
            val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: ""
            notificationUtil.notifyError(e, postTags, getString(R.string.upload_error_ticker), url.hashCode())
        }
    }

    private fun publish(intent: Intent, isDraft: Boolean) {
        val url = intent.getParcelableExtra<Uri>(EXTRA_URI) ?: return
        val selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME) ?: return
        val postTitle = intent.getStringExtra(EXTRA_POST_TITLE) ?: return
        val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: return

        try {
            if (isDraft) {
                TumblrManager.getInstance(applicationContext)
                    .draftPhotoPost(selectedBlogName, URI(url.toString()), postTitle, postTags)
            } else {
                TumblrManager.getInstance(applicationContext)
                    .publishPhotoPost(selectedBlogName, URI(url.toString()), postTitle, postTags)
            }
        } catch (e: Exception) {
            notificationUtil.notifyError(e, postTags, getString(R.string.retry),
                createRetryPublishIntent(intent), url.hashCode())
        }
    }

    private fun addBirthdateFromTags(intent: Intent): Disposable? {
        val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: return null
        val name = TumblrPost.tagsFromString(postTags).firstOrNull() ?: return null

        return ApiManager.birthdayService().getByName(name, true)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.response }
            .subscribe({ nameResult ->
                if (nameResult.isNew) {
                    notificationUtil.notifyBirthdayAdded(name, nameResult.birthday.birthdate)
                }
            }, { notificationUtil.notifyError(it, name, getString(R.string.birthday_add_error_ticker)) })
    }

    class RetryPublishNotificationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val url = intent.getParcelableExtra<Uri>(EXTRA_URI) ?: return
            val selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME) ?: return
            val postTitle = intent.getStringExtra(EXTRA_POST_TITLE) ?: return
            val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: return
            val action = intent.action
            val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)

            NotificationUtil(context).notificationManager.cancel(notificationTag, url.hashCode())

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
        fun startActionIntent(context: Context,
                              url: Uri,
                              blogName: String,
                              postTitle: String,
                              postTags: String,
                              publish: Boolean) {
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
