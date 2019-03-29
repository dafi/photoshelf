package com.ternaryop.photoshelf

import android.content.Context
import android.content.ContextWrapper
import android.os.Environment
import androidx.preference.PreferenceManager
import com.ternaryop.tumblr.Blog
import com.ternaryop.tumblr.android.TumblrManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

class AppSupport(context: Context) : ContextWrapper(context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    var selectedBlogName: String?
        get() = preferences.getString(PREF_SELECTED_BLOG, null)
        set(blogName) = preferences.edit().putString(PREF_SELECTED_BLOG, blogName).apply()

    val blogList: List<String>?
        get() {
            val blogSet = preferences.getStringSet(PREF_BLOG_NAMES, null) ?: return null
            val list = blogSet.toMutableList()
            list.sort()
            return list
        }

    val defaultScheduleMinutesTimeSpan: Int
        get() = preferences.getInt(PREF_SCHEDULE_MINUTES_TIME_SPAN,
            resources.getInteger(R.integer.schedule_minutes_time_span_default))

    var lastBirthdayShowTime: Long
        get() = preferences.getLong(LAST_BIRTHDAY_SHOW_TIME, 0)
        set(timems) = preferences.edit().putLong(LAST_BIRTHDAY_SHOW_TIME, timems).apply()

    val isAutomaticExportEnabled: Boolean
        get() = preferences.getBoolean(AUTOMATIC_EXPORT, false)

    val exportDaysPeriod: Int
        get() = preferences.getInt(PREF_EXPORT_DAYS_PERIOD, resources.getInteger(R.integer.export_days_period_default))

    var lastFollowersUpdateTime: Long
        get() = preferences.getLong(PREF_LAST_FOLLOWERS_UPDATE_TIME, -1)
        set(millisecs) = preferences.edit().putLong(PREF_LAST_FOLLOWERS_UPDATE_TIME, millisecs).apply()

    fun clearBlogList() {
        preferences.edit().remove(PREF_BLOG_NAMES).apply()
    }

    private fun setBlogList(blogNames: Set<String>) {
        preferences.edit().putStringSet(PREF_BLOG_NAMES, blogNames).apply()
    }

    fun fetchBlogNames(context: Context): Single<List<String>> {
        val blogList = blogList
        return if (blogList != null) {
            Single.fromCallable { blogList }
        } else Single
                .fromCallable { setupBlogs(TumblrManager.getInstance(context).blogList) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    private fun setupBlogs(blogs: Array<Blog>): List<String> {
        val blogNames = HashSet<String>(blogs.size)
        var primaryBlog: String? = null
        for (blog in blogs) {
            blogNames.add(blog.name)
            if (blog.isPrimary) {
                primaryBlog = blog.name
            }
        }
        setBlogList(blogNames)
        if (primaryBlog != null) {
            selectedBlogName = primaryBlog
        }
        return blogList!!
    }

    companion object {
        private const val SUBDIRECTORY_PICTURES = "TernaryOpPhotoShelf"
        private const val LAST_BIRTHDAY_SHOW_TIME = "lastBirthdayShowTime"
        private const val AUTOMATIC_EXPORT = "automatic_export"

        // Preferences keys
        const val PREF_SELECTED_BLOG = "selectedBlog"
        const val PREF_BLOG_NAMES = "blogNames"
        const val PREF_SCHEDULE_MINUTES_TIME_SPAN = "schedule_minutes_time_span"
        const val PREF_EXPORT_DAYS_PERIOD = "exportDaysPeriod"
        const val PREF_LAST_FOLLOWERS_UPDATE_TIME = "lastFollowersUpdateTime"

        val picturesDirectory: File
            get() {
                val fullDirPath = File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), SUBDIRECTORY_PICTURES)
                if (!fullDirPath.exists()) {
                    fullDirPath.mkdirs()
                }
                return fullDirPath
            }
    }
}
