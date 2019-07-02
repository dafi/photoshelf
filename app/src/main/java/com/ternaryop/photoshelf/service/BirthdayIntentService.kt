package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ternaryop.photoshelf.EXTRA_BIRTHDAY_DATE
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_BOOLEAN1
import com.ternaryop.photoshelf.EXTRA_LIST1
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.BirthdayResult
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.birthday.createBirthdayPost
import com.ternaryop.photoshelf.event.BirthdayEvent
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale

/**
 * Created by dave on 01/03/14.
 * Intent used to add, retrieve or update birthdays
 */
class BirthdayIntentService : IntentService("birthdayIntent") {

    private lateinit var notificationUtil: NotificationUtil

    private val birthdayBitmap: Bitmap
        @Throws(IOException::class)
        get() = assets.open("cake.png").use { stream -> return BitmapFactory.decodeStream(stream) }

    override fun onCreate() {
        super.onCreate()
        notificationUtil = NotificationUtil(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_BIRTHDAY_LIST_BY_DATE -> broadcastBirthdaysByDate(intent)
            ACTION_BIRTHDAY_PUBLISH -> birthdaysPublish(intent)
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
                tumblr.createBirthdayPost(cakeImage, bday, blogName, cacheDir, publishAsDraft)
            }
        } catch (e: Exception) {
            notificationUtil.notifyError(e, name, getString(R.string.birthday_publish_error_ticker, name, e.message))
        }
    }

    companion object {
        private const val ACTION_BIRTHDAY_LIST_BY_DATE = "birthdayListByDate"
        private const val ACTION_BIRTHDAY_PUBLISH = "birthdayPublish"

        fun startBirthdayListIntent(context: Context,
                                    date: Calendar,
                                    blogName: String) {
            val intent = Intent(context, BirthdayIntentService::class.java)
                .setAction(ACTION_BIRTHDAY_LIST_BY_DATE)
                .putExtra(EXTRA_BIRTHDAY_DATE, date)
                .putExtra(EXTRA_BLOG_NAME, blogName)

            context.startService(intent)
        }

        fun startPublishBirthdayIntent(context: Context,
                                       list: ArrayList<Birthday>,
                                       blogName: String,
                                       publishAsDraft: Boolean) {
            if (list.isEmpty()) {
                return
            }
            val intent = Intent(context, BirthdayIntentService::class.java)
                .setAction(ACTION_BIRTHDAY_PUBLISH)
                .putExtra(EXTRA_LIST1, list)
                .putExtra(EXTRA_BLOG_NAME, blogName)
                .putExtra(EXTRA_BOOLEAN1, publishAsDraft)

            context.startService(intent)
        }
    }
}
