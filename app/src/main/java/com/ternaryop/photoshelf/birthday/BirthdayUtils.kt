package com.ternaryop.photoshelf.birthday

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import com.ternaryop.photoshelf.api.birthday.BirthdayManager
import com.ternaryop.photoshelf.api.birthday.getClosestPhotoByWidth
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.util.network.ApiManager
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
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.net.URL
import java.util.Calendar
import java.util.Locale

private const val CAKE_IMAGE_SEPARATOR_HEIGHT = 10

object BirthdayUtils {
    fun notifyBirthday(context: Context) {
        val now = Calendar.getInstance(Locale.US)
        Single.fromCallable { ApiManager.birthdayManager(context).findByDate(
            BirthdayManager.FindParams(month = now.month + 1, dayOfMonth = now.dayOfMonth)) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ birthdayResult ->
                birthdayResult.birthdates?.let { list ->
                    if (list.isNotEmpty()) {
                        NotificationUtil(context).notifyTodayBirthdays(list, now.year)
                    }
                }
            }, { t -> NotificationUtil(context).notifyError(t, "Error") })
    }

    fun getPhotoPosts(context: Context, birthDate: Calendar, blogName: String): Observable<BirthdayManager.BirthdayResult> {
        return Observable
            .fromCallable {
                ApiManager.birthdayManager(context).findByDate(
                    BirthdayManager.FindParams(
                        month = birthDate.month + 1,
                        dayOfMonth = birthDate.dayOfMonth,
                        pickImages = true,
                        blogName = blogName))
            }
    }

    fun searchBirthday(context: Context, name: String, blogName: String): Birthday? {
        return try {
            val info = ApiManager.birthdayManager(context).getByName(name, true)
            Birthday(info.name, info.birthdate, blogName)
        } catch (ignored: Exception) {
            null
        }
    }

    fun createBirthdayPost(tumblr: Tumblr,
        cakeImage: Bitmap, birthday: BirthdayManager.Birthday, blogName: String, publishAsDraft: Boolean) {
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

    private fun getBirthdayCaption(birthday: BirthdayManager.Birthday): String {
        val age = birthday.birthdate.yearsBetweenDates()
        // caption must not be localized
        return "Happy ${age}th Birthday, ${birthday.name}!!"
    }
}
