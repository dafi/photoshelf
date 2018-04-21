package com.ternaryop.photoshelf.birthday

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.draftTextPost
import com.ternaryop.tumblr.publishPhotoPost
import com.ternaryop.utils.bitmap.readBitmap
import com.ternaryop.utils.bitmap.savePng
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors

private const val MAX_THUMB_COUNT = 9
private const val CAKE_IMAGE_SEPARATOR_HEIGHT = 10
private const val MAX_BIRTHDAY_THREAD_POOL_SIZE = 20

object BirthdayUtils {
    fun notifyBirthday(context: Context): Boolean {
        val birthdayDatabaseHelper = DBHelper.getInstance(context.applicationContext).birthdayDAO
        val now = Calendar.getInstance(Locale.US)
        val list = birthdayDatabaseHelper.getBirthdayByDate(now.time)
        if (list.isEmpty()) {
            return false
        }

        NotificationUtil(context).notifyTodayBirthdays(list, now.year)
        return true
    }

    fun getPhotoPosts(context: Context, birthDate: Calendar): Observable<Pair<Birthday, TumblrPhotoPost>> {
        val executorService = Executors.newFixedThreadPool(MAX_BIRTHDAY_THREAD_POOL_SIZE)
        val dbHelper = DBHelper.getInstance(context.applicationContext)
        val postTagDAO = dbHelper.postTagDAO
        val nullValue = Pair(Birthday("", null as Calendar?, ""), TumblrPhotoPost())
        val tumblr = TumblrManager.getInstance(context)

        return Observable
            .fromIterable(dbHelper.birthdayDAO.getBirthdayByDate(birthDate.time))
            .flatMap { b ->
                val postTag = postTagDAO.getRandomPostByTag(b.name, b.tumblrName)
                if (postTag == null) {
                    Observable.just(nullValue)
                } else {
                    val params = mapOf(
                        "type" to "photo",
                        "id" to postTag.id.toString())
                    Observable
                        .fromCallable { Pair(b, tumblr.getPublicPosts(b.tumblrName, params)[0] as TumblrPhotoPost) }
                        .subscribeOn(Schedulers.from(executorService))
                }
            }
            .filter { it.first.birthDate != null }
    }

    @Suppress("LongParameterList")
    fun publishedInAgeRange(context: Context,
        fromAge: Int, toAge: Int, daysPeriod: Int, postTags: String, tumblrName: String) {
        require(fromAge != 0 && toAge != 0) { "fromAge or toAge can't be both set to 0" }
        var message = if (fromAge == 0) {
            context.getString(R.string.week_selection_under_age, toAge)
        } else {
            context.getString(R.string.week_selection_over_age, fromAge)
        }

        val dbHelper = DBHelper.getInstance(context)
        val birthdays = dbHelper.birthdayDAO
                .getBirthdayByAgeRange(fromAge, if (toAge == 0) Integer.MAX_VALUE else toAge, daysPeriod, tumblrName)
                .shuffled()

        val sb = StringBuilder()

        val params = HashMap<String, String>(2)
        var post: TumblrPhotoPost? = null
        params["type"] = "photo"

        for ((count, info) in birthdays.withIndex()) {
            params["id"] = info["postId"]!!
            post = TumblrManager.getInstance(context)
                    .getPublicPosts(tumblrName, params)[0] as TumblrPhotoPost
            val imageUrl = post.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_250)!!.url

            sb.append("<a href=\"")
                    .append(post.postUrl)
                    .append("\">")
            sb.append("<p>")
                    .append(context.getString(R.string.name_with_age, post.tags[0], info["age"]))
                    .append("</p>")
            sb.append("<img style=\"width: 250px !important\" src=\"")
                    .append(imageUrl)
                    .append("\"/>")
            sb.append("</a>")
            sb.append("<br/>")

            if (count + 1 > MAX_THUMB_COUNT) {
                break
            }
        }
        if (post != null) {
            message = message + " (" + formatPeriodDate(-daysPeriod) + ")"

            TumblrManager.getInstance(context)
                    .draftTextPost(tumblrName,
                            message,
                            sb.toString(),
                            postTags)
        }
    }

    private fun formatPeriodDate(daysPeriod: Int): String {
        val now = Calendar.getInstance()
        val period = Calendar.getInstance()
        period.add(Calendar.DAY_OF_MONTH, daysPeriod)
        val sdf = SimpleDateFormat("MMMM", Locale.US)

        return if (now.year == period.year) {
            if (now.month == period.month) {
                period.dayOfMonth.toString() + "-" + now.dayOfMonth + " " + sdf.format(now.time) + ", " + now.year
            } else (period.dayOfMonth.toString() + " " + sdf.format(period.time)
                    + " - " + now.dayOfMonth + " " + sdf.format(now.time)
                    + ", " + now.year)
        } else (period.dayOfMonth.toString() + " " + sdf.format(period.time) + " " + period.year
                + " - " + now.dayOfMonth + " " + sdf.format(now.time) + " " + now.year)
    }

    fun searchBirthday(context: Context, name: String, blogName: String): Birthday? {
        return try {
            val info = BirthdayManager(context.getString(R.string.PHOTOSHELF_EXTRACTOR_ACCESS_TOKEN)).search(name)
            Birthday(info.name, info.birthdate, blogName)
        } catch (ignored: Exception) {
            null
        }
    }

    fun createBirthdayPost(context: Context,
        cakeImage: Bitmap, post: TumblrPhotoPost, blogName: String, publishAsDraft: Boolean) {
        val name = post.tags[0]
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "birth-$name.png")
        val imageUrl = post.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_400)!!.url
        file.savePng(createBirthdayBitmap(cakeImage, URL(imageUrl).readBitmap()))
        try {
            if (publishAsDraft) {
                TumblrManager.getInstance(context).draftPhotoPost(blogName,
                    file.toURI(), getBirthdayCaption(context, name, blogName), "Birthday, $name")
            } else {
                TumblrManager.getInstance(context).publishPhotoPost(blogName,
                    file.toURI(), getBirthdayCaption(context, name, blogName), "Birthday, $name")
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

    private fun getBirthdayCaption(context: Context, name: String, blogName: String): String {
        val dbHelper = DBHelper.getInstance(context.applicationContext)
        val birthDay = dbHelper.birthdayDAO.getBirthdayByName(name, blogName)
        val age = Calendar.getInstance().year - birthDay!!.birthYear
        // caption must not be localized
        return "Happy ${age}th Birthday, $name!!"
    }
}
