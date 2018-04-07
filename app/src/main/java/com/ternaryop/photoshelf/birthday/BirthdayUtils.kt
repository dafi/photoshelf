package com.ternaryop.photoshelf.birthday

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrAltSize
import com.ternaryop.tumblr.TumblrPhotoPost
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.draftTextPost
import com.ternaryop.tumblr.publishPhotoPost
import com.ternaryop.utils.bitmap.readBitmap
import com.ternaryop.utils.bitmap.savePng
import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale

const val MAX_THUMB_COUNT = 9
const val CAKE_IMAGE_SEPARATOR_HEIGHT = 10

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

    fun getPhotoPosts(context: Context, birthDate: Calendar): List<Pair<Birthday, TumblrPhotoPost>> {
        val dbHelper = DBHelper.getInstance(context.applicationContext)
        val birthDays = dbHelper.birthdayDAO.getBirthdayByDate(birthDate.time)
        val posts = mutableListOf<Pair<Birthday, TumblrPhotoPost>>()

        val postTagDAO = dbHelper.postTagDAO
        val params = HashMap<String, String>(2)
        params["type"] = "photo"
        for (b in birthDays) {
            val postTag = postTagDAO.getRandomPostByTag(b.name, b.tumblrName)
            if (postTag != null) {
                params["id"] = postTag.id.toString()
                val post = Tumblr.getSharedTumblr(context).getPublicPosts(b.tumblrName, params)[0] as TumblrPhotoPost
                posts.add(Pair(b, post))
            }
        }
        return posts
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
        Collections.shuffle(birthdays)

        val sb = StringBuilder()

        val params = HashMap<String, String>(2)
        var post: TumblrPhotoPost? = null
        params["type"] = "photo"

        for ((count, info) in birthdays.withIndex()) {
            params["id"] = info["postId"]!!
            post = Tumblr.getSharedTumblr(context)
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

            Tumblr.getSharedTumblr(context)
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
        cakeImage: Bitmap, post: TumblrPhotoPost, blogName: String, saveAsDraft: Boolean) {
        val imageUrl = post.getClosestPhotoByWidth(TumblrAltSize.IMAGE_WIDTH_400)!!.url
        val image = URL(imageUrl).readBitmap()

        val canvasWidth = image.width
        val canvasHeight = cakeImage.height + CAKE_IMAGE_SEPARATOR_HEIGHT + image.height

        val config = image.config ?: Bitmap.Config.ARGB_8888
        val destBmp = Bitmap.createBitmap(canvasWidth, canvasHeight, config)
        val canvas = Canvas(destBmp)

        canvas.drawBitmap(cakeImage, ((image.width - cakeImage.width) / 2).toFloat(), 0f, null)
        canvas.drawBitmap(image, 0f, (cakeImage.height + CAKE_IMAGE_SEPARATOR_HEIGHT).toFloat(), null)
        val name = post.tags[0]
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "birth-$name.png")
        file.savePng(destBmp)
        if (saveAsDraft) {
            Tumblr.getSharedTumblr(context).draftPhotoPost(blogName,
                    Uri.fromFile(file),
                    getBirthdayCaption(context, name, blogName),
                "Birthday, $name")
        } else {
            Tumblr.getSharedTumblr(context).publishPhotoPost(blogName,
                    Uri.fromFile(file),
                    getBirthdayCaption(context, name, blogName),
                "Birthday, $name")
        }
        file.delete()
    }

    private fun getBirthdayCaption(context: Context, name: String, blogName: String): String {
        val dbHelper = DBHelper.getInstance(context.applicationContext)
        val birthDay = dbHelper.birthdayDAO.getBirthdayByName(name, blogName)
        val age = Calendar.getInstance().year - birthDay!!.birthYear
        // caption must not be localized
        return "Happy ${age}th Birthday, $name!!"
    }
}
