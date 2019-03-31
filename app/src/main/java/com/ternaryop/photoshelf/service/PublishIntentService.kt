package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.widget.Toast
import androidx.annotation.StringRes
import com.ternaryop.photoshelf.EXTRA_ACTION
import com.ternaryop.photoshelf.EXTRA_BIRTHDAY_DATE
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_BOOLEAN1
import com.ternaryop.photoshelf.EXTRA_LIST1
import com.ternaryop.photoshelf.EXTRA_NOTIFICATION_TAG
import com.ternaryop.photoshelf.EXTRA_POST_TAGS
import com.ternaryop.photoshelf.EXTRA_POST_TITLE
import com.ternaryop.photoshelf.EXTRA_URI
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.BirthdayResult
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.event.BirthdayEvent
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.publishPhotoPost
import com.ternaryop.utils.bitmap.readBitmap
import com.ternaryop.utils.bitmap.scale
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.log.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URL
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale

/**
 * Created by dave on 01/03/14.
 * Contains all methods used to publish posts
 */
class PublishIntentService : IntentService("publishIntent") {

    private lateinit var notificationUtil: NotificationUtil
    private val handler = Handler()

    private val birthdayBitmap: Bitmap
        @Throws(IOException::class)
        get() = assets.open("cake.png").use { stream -> return BitmapFactory.decodeStream(stream) }

    override fun onCreate() {
        super.onCreate()
        notificationUtil = NotificationUtil(this)
    }

    @Suppress("ComplexMethod")
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val url = intent.getParcelableExtra<Uri>(EXTRA_URI) ?: return
        val selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME) ?: return
        val postTitle = intent.getStringExtra(EXTRA_POST_TITLE) ?: return
        val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: return
        val action = intent.getStringExtra(EXTRA_ACTION) ?: return

        try {
            addBirthdateFromTags(postTags)
            when (action) {
                ACTION_PUBLISH_DRAFT -> TumblrManager.getInstance(applicationContext)
                    .draftPhotoPost(selectedBlogName, URI(url.toString()), postTitle, postTags)
                ACTION_PUBLISH_PUBLISH -> TumblrManager.getInstance(applicationContext)
                    .publishPhotoPost(selectedBlogName, URI(url.toString()), postTitle, postTags)
                ACTION_BIRTHDAY_LIST_BY_DATE -> broadcastBirthdaysByDate(intent)
                ACTION_BIRTHDAY_PUBLISH -> birthdaysPublish(intent)
                ACTION_CHANGE_WALLPAPER -> changeWallpaper(url)
            }
        } catch (e: Exception) {
            logError(intent, e)
            when (action) {
                ACTION_PUBLISH_DRAFT, ACTION_PUBLISH_PUBLISH ->
                    notificationUtil.notifyError(e, postTags, getString(R.string.retry),
                        createRetryPublishIntent(intent), url.hashCode())
                else ->
                    notificationUtil.notifyError(e, postTags, getString(R.string.upload_error_ticker), url.hashCode())
            }
        }
    }

    private fun changeWallpaper(imageUrl: Uri) {
        try {
            val metrics = resources.displayMetrics
            val bitmap = URL(imageUrl.toString())
                .readBitmap().scale(metrics.widthPixels, metrics.heightPixels, true)
            WallpaperManager.getInstance(this).setBitmap(bitmap)
            showToast(R.string.wallpaper_changed_title)
        } catch (e: Exception) {
            notificationUtil.notifyError(e, "")
        }
    }

    private fun addBirthdateFromTags(postTags: String): Disposable? {
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

    private fun logError(intent: Intent, e: Exception) {
        val url = intent.getParcelableExtra<Uri>(EXTRA_URI)
        val postTags = intent.getStringExtra(EXTRA_POST_TAGS)

        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "publish_errors.txt")
            Log.error(e, file, " Error on url $url", " tags $postTags")
        } catch (ignored: Exception) {
        }
    }

    private fun broadcastBirthdaysByDate(intent: Intent): Disposable? {
        val birthday = intent.getSerializableExtra(EXTRA_BIRTHDAY_DATE) as Calendar? ?: Calendar.getInstance(Locale.US)
        val blogName = intent.getStringExtra(EXTRA_BLOG_NAME)

        return ApiManager.birthdayService().findByDate(
            FindParams(
                month = birthday.month + 1,
                dayOfMonth = birthday.dayOfMonth,
                pickImages = true,
                blogName = blogName).toQueryMap())
            .map { it.response }
            .subscribeOn(Schedulers.io())
            .doFinally { if (EventBus.getDefault().hasSubscriberForEvent(BirthdayEvent::class.java))
                EventBus.getDefault().post(BirthdayEvent()) }
            .subscribe(Consumer<BirthdayResult> {
                if (EventBus.getDefault().hasSubscriberForEvent(BirthdayEvent::class.java))
                    EventBus.getDefault().post(BirthdayEvent(it))
            })
    }

    @Suppress("UNCHECKED_CAST")
    private fun birthdaysPublish(intent: Intent) {
        val list = intent.getSerializableExtra(EXTRA_LIST1) as List<Birthday>
        val blogName = intent.getStringExtra(EXTRA_BLOG_NAME) ?: return
        val publishAsDraft = intent.getBooleanExtra(EXTRA_BOOLEAN1, true)
        var name = ""

        try {
            val cakeImage = birthdayBitmap
            val tumblr = TumblrManager.getInstance(applicationContext)
            for (bday in list) {
                name = bday.name
                BirthdayUtils.createBirthdayPost(tumblr, cakeImage, bday, blogName, publishAsDraft)
            }
        } catch (e: Exception) {
            logError(intent, e)
            notificationUtil.notifyError(e, name, getString(R.string.birthday_publish_error_ticker, name, e.message))
        }
    }

    private fun showToast(@StringRes res: Int) {
        handler.post { Toast.makeText(applicationContext, res, Toast.LENGTH_LONG).show() }
    }

    class RetryPublishNotificationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val url = intent.getParcelableExtra<Uri>(EXTRA_URI) ?: return
            val selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME) ?: return
            val postTitle = intent.getStringExtra(EXTRA_POST_TITLE) ?: return
            val postTags = intent.getStringExtra(EXTRA_POST_TAGS) ?: return
            val action = intent.getStringExtra(EXTRA_ACTION)
            val notificationTag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG)

            NotificationUtil(context).notificationManager.cancel(notificationTag, url.hashCode())

            PublishIntentService.startActionIntent(context,
                url,
                selectedBlogName,
                postTitle,
                postTags,
                action == ACTION_PUBLISH_PUBLISH)
        }
    }

    private fun createRetryPublishIntent(failedIntent: Intent): Intent {
        val intent = Intent(this, RetryPublishNotificationBroadcastReceiver::class.java)
        intent.action = RETRY_PUBLISH_ACTION

        intent.putExtra(EXTRA_URI, failedIntent.getParcelableExtra<Uri>(EXTRA_URI))
        intent.putExtra(EXTRA_BLOG_NAME, failedIntent.getStringExtra(EXTRA_BLOG_NAME))
        intent.putExtra(EXTRA_POST_TITLE, failedIntent.getStringExtra(EXTRA_POST_TITLE))
        intent.putExtra(EXTRA_POST_TAGS, failedIntent.getStringExtra(EXTRA_POST_TAGS))
        intent.putExtra(EXTRA_ACTION, failedIntent.getStringExtra(EXTRA_ACTION))

        return intent
    }

    companion object {
        private const val ACTION_PUBLISH_DRAFT = "draft"
        private const val ACTION_PUBLISH_PUBLISH = "publish"
        private const val ACTION_BIRTHDAY_LIST_BY_DATE = "birthdayListByDate"
        private const val ACTION_BIRTHDAY_PUBLISH = "birthdayPublish"
        private const val ACTION_CHANGE_WALLPAPER = "changeWallpaper"

        private const val RETRY_PUBLISH_ACTION = "com.ternaryop.photoshelf.publish.retry"

        @Suppress("LongParameterList")
        fun startActionIntent(context: Context,
                              url: Uri,
                              blogName: String,
                              postTitle: String,
                              postTags: String,
                              publish: Boolean) {
            val intent = Intent(context, PublishIntentService::class.java)
            intent.putExtra(EXTRA_URI, url)
            intent.putExtra(EXTRA_BLOG_NAME, blogName)
            intent.putExtra(EXTRA_POST_TITLE, postTitle)
            intent.putExtra(EXTRA_POST_TAGS, postTags)
            intent.putExtra(EXTRA_ACTION, if (publish) ACTION_PUBLISH_PUBLISH else ACTION_PUBLISH_DRAFT)

            context.startService(intent)
        }

        fun startBirthdayListIntent(context: Context,
                                    date: Calendar,
                                    blogName: String) {
            val intent = Intent(context, PublishIntentService::class.java)
            intent.putExtra(EXTRA_BIRTHDAY_DATE, date)
            intent.putExtra(EXTRA_BLOG_NAME, blogName)
            intent.putExtra(EXTRA_ACTION, ACTION_BIRTHDAY_LIST_BY_DATE)

            context.startService(intent)
        }

        fun startPublishBirthdayIntent(context: Context,
                                       list: ArrayList<Birthday>,
                                       blogName: String,
                                       publishAsDraft: Boolean) {
            if (list.isEmpty()) {
                return
            }
            val intent = Intent(context, PublishIntentService::class.java)
            intent.putExtra(EXTRA_LIST1, list)
            intent.putExtra(EXTRA_BLOG_NAME, blogName)
            intent.putExtra(EXTRA_BOOLEAN1, publishAsDraft)
            intent.putExtra(EXTRA_ACTION, ACTION_BIRTHDAY_PUBLISH)

            context.startService(intent)
        }

        fun startChangeWallpaperIntent(context: Context,
                                       imageUri: Uri) {
            val intent = Intent(context, PublishIntentService::class.java)
            intent.putExtra(EXTRA_URI, imageUri)
            intent.putExtra(EXTRA_ACTION, ACTION_CHANGE_WALLPAPER)

            context.startService(intent)
        }
    }
}
