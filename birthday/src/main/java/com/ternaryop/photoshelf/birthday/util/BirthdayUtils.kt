package com.ternaryop.photoshelf.birthday.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.api.birthday.FindParams
import com.ternaryop.photoshelf.api.birthday.getClosestPhotoByWidth
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.birthday.publisher.activity.BirthdayPublisherActivity
import com.ternaryop.photoshelf.birthday.util.notification.BirthdayNotificationBroadcastReceiver
import com.ternaryop.photoshelf.birthday.util.notification.notifyTodayBirthdays
import com.ternaryop.photoshelf.util.notification.notify
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
import java.io.File
import java.net.URL
import java.util.Calendar
import java.util.Locale

private const val CAKE_IMAGE_SEPARATOR_HEIGHT = 10

suspend fun notifyBirthday(context: Context, blogName: String? = null) {
    try {
        val now = Calendar.getInstance(Locale.US)
        val birthdayResult = ApiManager.birthdayService().findByDate(
            FindParams(month = now.month + 1, dayOfMonth = now.dayOfMonth, blogName = blogName).toQueryMap()).response
        birthdayResult.birthdays?.notifyTodayBirthdays(context, now.year, BirthdayPublisherActivity::class.java)
    } catch (t: Throwable) {
        t.notify(context, "Error")
    }
}

fun createBirthdayImageFile(cakeImage: Bitmap, birthday: Birthday, cacheDir: File): File {
    return File(cacheDir, "birth-${birthday.name}.png").also { file ->
        val imageUrl = checkNotNull(birthday.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_400)).url
        file.savePng(createBirthdayBitmap(cakeImage, URL(imageUrl).readBitmap()))
    }
}

fun Tumblr.createBirthdayPost(file: File, birthday: Birthday, blogName: String, publishAsDraft: Boolean) {
    try {
        if (publishAsDraft) {
            draftPhotoPost(blogName, file.toURI(), getBirthdayCaption(birthday), "Birthday, ${birthday.name}")
        } else {
            publishPhotoPost(blogName, file.toURI(), getBirthdayCaption(birthday), "Birthday, ${birthday.name}")
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

suspend fun addBirthdate(context: Context, name: String) {
    try {
        val nameResult = ApiManager.birthdayService().getByName(name, true).response
        if (nameResult.isNew) {
            BirthdayNotificationBroadcastReceiver.notifyBirthdayAdded(context, name, nameResult.birthday.birthdate)
        }
    } catch (t: Throwable) {
        t.notify(context, name, context.resources.getString(R.string.birthday_add_error_ticker))
    }
}
