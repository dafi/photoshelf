package com.ternaryop.photoshelf.birthday

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.api.birthday.getClosestPhotoByWidth
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.publishPhotoPost
import com.ternaryop.utils.bitmap.readBitmap
import com.ternaryop.utils.bitmap.savePng
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import com.ternaryop.utils.date.yearsBetweenDates
import com.ternaryop.utils.log.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.net.URL
import java.util.Calendar
import java.util.Locale

private const val CAKE_IMAGE_SEPARATOR_HEIGHT = 10

object BirthdayUtils {
    fun notifyBirthday(context: Context): Disposable? {
        val now = Calendar.getInstance(Locale.US)
        return ApiManager.birthdayService(context).findByDate(
            FindParams(month = now.month + 1, dayOfMonth = now.dayOfMonth).toQueryMap())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.response }
            .subscribe({ birthdayResult ->
                birthdayResult.birthdays?.let { list ->
                    if (list.isNotEmpty()) {
                        NotificationUtil(context).notifyTodayBirthdays(list, now.year)
                    }
                }
            }, { t ->
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "publish_errors.txt")
                Log.error(t, file)
                NotificationUtil(context).notifyError(t, "Error") })
    }

    fun createBirthdayPost(tumblr: Tumblr,
        cakeImage: Bitmap, birthday: Birthday, blogName: String, publishAsDraft: Boolean) {
        val name = birthday.name
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "birth-$name.png")
        val imageUrl = birthday.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_400)!!.url
        file.savePng(createBirthdayBitmap(cakeImage, URL(imageUrl).readBitmap()))
        try {
            if (publishAsDraft) {
                tumblr.draftPhotoPost(blogName,
                    file.toURI(), getBirthdayCaption(birthday), "Birthday, $name")
            } else {
                tumblr.publishPhotoPost(blogName,
                    file.toURI(), getBirthdayCaption(birthday), "Birthday, $name")
            }
        } finally {
            file.delete()
        }
    }

    private fun createBirthdayBitmap(cake: Bitmap, celebrity: Bitmap): Bitmap {
        val canvasWidth = celebrity.width
        val canvasHeight = cake.height + CAKE_IMAGE_SEPARATOR_HEIGHT + celebrity.height

        val config = celebrity.config ?: Bitmap.Config.ARGB_8888
        val destBmp = Bitmap.createBitmap(canvasWidth, canvasHeight, config)
        val canvas = Canvas(destBmp)

        canvas.drawBitmap(cake, ((celebrity.width - cake.width) / 2).toFloat(), 0f, null)
        canvas.drawBitmap(celebrity, 0f, (cake.height + CAKE_IMAGE_SEPARATOR_HEIGHT).toFloat(), null)

        return destBmp
    }

    private fun getBirthdayCaption(birthday: Birthday): String {
        val age = birthday.birthdate.yearsBetweenDates()
        // caption must not be localized
        return "Happy ${age}th Birthday, ${birthday.name}!!"
    }
}
