package com.ternaryop.photoshelf.birthday.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_BOOLEAN1
import com.ternaryop.photoshelf.EXTRA_LIST1
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.birthday.util.createBirthdayImageFile
import com.ternaryop.photoshelf.birthday.util.createBirthdayPost
import com.ternaryop.photoshelf.service.AbsIntentService
import com.ternaryop.photoshelf.util.notification.notify
import com.ternaryop.tumblr.android.TumblrManager
import java.io.IOException
import java.util.ArrayList

/**
 * Created by dave on 01/03/14.
 * Intent used to add, retrieve or update birthdays
 */
class BirthdayIntentService : AbsIntentService("birthdayIntent") {
    private val birthdayBitmap: Bitmap
        @Throws(IOException::class)
        get() = assets.open("cake.png").use { stream -> return BitmapFactory.decodeStream(stream) }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_BIRTHDAY_PUBLISH -> birthdaysPublish(intent)
        }
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
                val imageFile = createBirthdayImageFile(cakeImage, bday, cacheDir)
                tumblr.createBirthdayPost(imageFile, bday, blogName, publishAsDraft)
            }
        } catch (e: Exception) {
            e.notify(this, name, getString(R.string.birthday_publish_error_ticker, name, e.message))
        }
    }

    companion object {
        private const val ACTION_BIRTHDAY_PUBLISH = "birthdayPublish"

        fun startPublishBirthdayIntent(
            context: Context,
            list: ArrayList<Birthday>,
            blogName: String,
            publishAsDraft: Boolean
        ) {
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
