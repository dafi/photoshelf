package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.support.annotation.StringRes
import android.widget.Toast
import com.ternaryop.photoshelf.EXTRA_ACTION
import com.ternaryop.photoshelf.EXTRA_BIRTHDAY_DATE
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_BOOLEAN1
import com.ternaryop.photoshelf.EXTRA_LIST1
import com.ternaryop.photoshelf.EXTRA_POST_TAGS
import com.ternaryop.photoshelf.EXTRA_POST_TITLE
import com.ternaryop.photoshelf.EXTRA_URI
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.BirthdayDAO
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.event.BirthdayEvent
import com.ternaryop.photoshelf.util.log.Log
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.publishPhotoPost
import com.ternaryop.utils.bitmap.readBitmap
import com.ternaryop.utils.bitmap.scale
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.net.URL
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
        val url = intent.getParcelableExtra<Uri>(EXTRA_URI)
        val selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME)
        val postTitle = intent.getStringExtra(EXTRA_POST_TITLE)
        val postTags = intent.getStringExtra(EXTRA_POST_TAGS)
        val action = intent.getStringExtra(EXTRA_ACTION)

        try {
            if (postTags != null) {
                addBirthdateFromTags(postTags, selectedBlogName)
            }
            when {
                ACTION_PUBLISH_DRAFT == action -> Tumblr.getSharedTumblr(applicationContext)
                    .draftPhotoPost(selectedBlogName, url, postTitle, postTags)
                ACTION_PUBLISH_PUBLISH == action -> Tumblr.getSharedTumblr(applicationContext)
                    .publishPhotoPost(selectedBlogName, url, postTitle, postTags)
                ACTION_BIRTHDAY_LIST_BY_DATE == action -> broadcastBirthdaysByDate(intent)
                ACTION_BIRTHDAY_PUBLISH == action -> birthdaysPublish(intent)
                ACTION_CHANGE_WALLPAPER == action -> changeWallpaper(url)
            }
        } catch (e: Exception) {
            logError(intent, e)
            notificationUtil.notifyError(e, postTags, getString(R.string.upload_error_ticker), url.hashCode())
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

    private fun addBirthdateFromTags(postTags: String, blogName: String) {
        val name = TumblrPost.tagsFromString(postTags).firstOrNull() ?: return

        Thread(Runnable {
            val birthdayDAO = DBHelper.getInstance(applicationContext).birthdayDAO
            val db = birthdayDAO.dbHelper.writableDatabase
            db.beginTransaction()

            try {
                val birthday = searchMissingBirthday(birthdayDAO, name, blogName)
                if (birthday != null) {
                    birthdayDAO.insert(birthday)
                    db.setTransactionSuccessful()
                    notificationUtil.notifyBirthdayAdded(name, birthday.birthDate!!)
                }
            } catch (e: Exception) {
                notificationUtil.notifyError(e, name, getString(R.string.birthday_add_error_ticker))
            } finally {
                db.endTransaction()
            }
        }).start()
    }

    private fun searchMissingBirthday(birthdayDAO: BirthdayDAO, name: String, blogName: String): Birthday? {
        if (birthdayDAO.getBirthdayByName(name, blogName) != null) {
            return null
        }
        return BirthdayUtils.searchBirthday(applicationContext, name, blogName)
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

    private fun broadcastBirthdaysByDate(intent: Intent) {
        val birthday = intent.getSerializableExtra(EXTRA_BIRTHDAY_DATE) as Calendar? ?: Calendar.getInstance(Locale.US)
        val list: List<Pair<Birthday, TumblrPhotoPost>> = try {
            BirthdayUtils.getPhotoPosts(applicationContext, birthday)
        } catch (ex: Exception) {
            emptyList()
        }

        if (EventBus.getDefault().hasSubscriberForEvent(BirthdayEvent::class.java)) {
            EventBus.getDefault().post(BirthdayEvent(list))
        }
    }

    private fun birthdaysPublish(intent: Intent) {
        val posts = intent.getSerializableExtra(EXTRA_LIST1) as List<TumblrPhotoPost>
        val blogName = intent.getStringExtra(EXTRA_BLOG_NAME)
        val publishAsDraft = intent.getBooleanExtra(EXTRA_BOOLEAN1, true)
        var name = ""

        try {
            val cakeImage = birthdayBitmap
            for (post in posts) {
                name = post.tags[0]
                BirthdayUtils.createBirthdayPost(applicationContext, cakeImage, post, blogName, publishAsDraft)
            }
        } catch (e: Exception) {
            logError(intent, e)
            notificationUtil.notifyError(e, name, getString(R.string.birthday_publish_error_ticker, name, e.message))
        }
    }

    private fun showToast(@StringRes res: Int) {
        handler.post { Toast.makeText(applicationContext, res, Toast.LENGTH_LONG).show() }
    }

    companion object {
        private const val ACTION_PUBLISH_DRAFT = "draft"
        private const val ACTION_PUBLISH_PUBLISH = "publish"
        private const val ACTION_BIRTHDAY_LIST_BY_DATE = "birthdayListByDate"
        private const val ACTION_BIRTHDAY_PUBLISH = "birthdayPublish"
        private const val ACTION_CHANGE_WALLPAPER = "changeWallpaper"

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
                                    date: Calendar) {
            val intent = Intent(context, PublishIntentService::class.java)
            intent.putExtra(EXTRA_BIRTHDAY_DATE, date)
            intent.putExtra(EXTRA_ACTION, ACTION_BIRTHDAY_LIST_BY_DATE)

            context.startService(intent)
        }

        fun startPublishBirthdayIntent(context: Context,
                                       list: ArrayList<TumblrPhotoPost>,
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
