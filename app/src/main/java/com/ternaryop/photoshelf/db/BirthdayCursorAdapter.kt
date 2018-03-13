package com.ternaryop.photoshelf.db

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.Filter
import android.widget.FilterQueryProvider
import android.widget.SimpleCursorAdapter
import android.widget.SimpleCursorAdapter.ViewBinder
import android.widget.TextView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.TagPhotoBrowserActivity
import com.ternaryop.photoshelf.util.date.dayOfMonth
import com.ternaryop.photoshelf.util.date.month
import com.ternaryop.photoshelf.util.text.fromHtml
import com.ternaryop.utils.DateTimeUtils
import com.ternaryop.utils.StringUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Used by searchView in actionBar
 * @author dave
 */
@Suppress("MemberVisibilityCanBePrivate")
class BirthdayCursorAdapter(private val context: Context,
    var blogName: String) : SimpleCursorAdapter(context, R.layout.list_row_2,
    null,
    arrayOf(BirthdayDAO.NAME, BirthdayDAO.BIRTH_DATE),
    intArrayOf(android.R.id.text1, android.R.id.text2), 0), FilterQueryProvider, ViewBinder {

    private val birthdayDAO = DBHelper.getInstance(context).birthdayDAO
    private val dateFormat: SimpleDateFormat = SimpleDateFormat("d MMMM, yyyy", Locale.US)
    private var showFlags = SHOW_ALL
    private var pattern = ""
    var month: Int = 0

    val isShowIgnored: Boolean
        get() = showFlags and SHOW_IGNORED != 0

    val isShowInSameDay: Boolean
        get() = showFlags and SHOW_IN_SAME_DAY != 0

    val isShowMissing: Boolean
        get() = showFlags and SHOW_MISSING != 0

    val isWithoutPost: Boolean
        get() = showFlags and SHOW_WITHOUT_POSTS != 0

    init {
        viewBinder = this
        filterQueryProvider = this
    }

    override fun runQuery(constraint: CharSequence?): Cursor {
        this.pattern = constraint?.toString()?.trim { it <= ' ' } ?: ""
        return when {
            isShowIgnored -> birthdayDAO.getIgnoredBirthdayCursor(pattern, blogName)
            isShowInSameDay -> birthdayDAO.getBirthdaysInSameDay(pattern, blogName)
            isShowMissing -> birthdayDAO.getMissingBirthDaysCursor(pattern, blogName)
            isWithoutPost -> birthdayDAO.getBirthdaysWithoutPostsCursor(blogName)
            else -> birthdayDAO.getBirthdayCursorByName(pattern, month, blogName)
        }
    }

    override fun convertToString(cursor: Cursor): String {
        val columnIndex = cursor.getColumnIndexOrThrow(BirthdayDAO.NAME)
        return cursor.getString(columnIndex)
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        super.bindView(view, context, cursor)
        val now = Calendar.getInstance()

        try {
            val isoDate = cursor.getString(cursor.getColumnIndex(BirthdayDAO.BIRTH_DATE))
            if (isoDate == null) {
                view.setBackgroundResource(R.drawable.list_selector_post_group_even)
            } else {
                val date = Birthday.fromIsoFormat(isoDate)
                if (date.dayOfMonth == now.dayOfMonth && date.month == now.month) {
                    view.setBackgroundResource(R.drawable.list_selector_post_never)
                } else {
                    // group by day, not perfect by better than nothing
                    val isEven = date.dayOfMonth and 1 == 0
                    view.setBackgroundResource(if (isEven) R.drawable.list_selector_post_group_even else R.drawable.list_selector_post_group_odd)
                }
            }
        } catch (ignored: ParseException) {
        }
    }

    override fun setViewValue(view: View, cursor: Cursor, columnIndex: Int): Boolean {
        if (columnIndex == cursor.getColumnIndexOrThrow(BirthdayDAO.NAME)) {
            setViewValueName(view, cursor, columnIndex)
        } else if (columnIndex == cursor.getColumnIndexOrThrow(BirthdayDAO.BIRTH_DATE)) {
            setViewValueBirthdate(view, cursor, columnIndex)
        }
        return true
    }

    private fun setViewValueName(view: View, cursor: Cursor, columnIndex: Int) {
        if (pattern.isEmpty()) {
            (view as TextView).text = cursor.getString(columnIndex)
        } else {
            (view as TextView).text = StringUtils.htmlHighlightPattern(
                pattern, cursor.getString(columnIndex)).fromHtml()
        }
    }

    private fun setViewValueBirthdate(view: View, cursor: Cursor, columnIndex: Int) {
        try {
            val isoDate = cursor.getString(columnIndex)
            if (isoDate == null) {
                view.visibility = View.GONE
            } else {
                val c = Birthday.fromIsoFormat(isoDate)
                val age = DateTimeUtils.yearsBetweenDates(c, Calendar.getInstance()).toString()
                val dateStr = dateFormat.format(c.time)

                view.visibility = View.VISIBLE
                (view as TextView).text = context.getString(R.string.name_with_age, dateStr, age)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }

    fun browsePhotos(activity: Activity, position: Int) {
        val tag = cursor.getString(cursor.getColumnIndex(BirthdayDAO.NAME))
        TagPhotoBrowserActivity.startPhotoBrowserActivity(activity, blogName, tag, false)
    }

    fun getBirthdayItem(index: Int): Birthday? {
        return BirthdayDAO.getBirthday(getItem(index) as Cursor)
    }

    fun refresh(filterListener: Filter.FilterListener? = null) {
        filter.filter(pattern, filterListener)
        notifyDataSetChanged()
    }

    fun findDayPosition(day: Int): Int {
        if (day !in dayRange) {
            return -1
        }
        for (i in 0 until count) {
            val cursor = getItem(i) as Cursor
            val isoDate = cursor.getString(cursor.getColumnIndex(BirthdayDAO.BIRTH_DATE)) ?: continue

            val bday = Integer.parseInt(isoDate.substring(isoDate.length - 2))

            // move to bday or closest one
            if (bday >= day) {
                return i
            }
        }
        return -1
    }

    fun isShowFlag(value: Int): Boolean {
        return showFlags and value != 0
    }

    @Suppress("ComplexMethod")
    fun setShow(value: Int, show: Boolean) {
        // SHOW_ALL is the default value and it can't be hidden
        showFlags = when {
            value and SHOW_ALL != 0 -> SHOW_ALL
            value and SHOW_IGNORED != 0 -> if (show) SHOW_IGNORED else SHOW_ALL
            value and SHOW_IN_SAME_DAY != 0 -> if (show) SHOW_IN_SAME_DAY else SHOW_ALL
            value and SHOW_MISSING != 0 -> if (show) SHOW_MISSING else SHOW_ALL
            value and SHOW_WITHOUT_POSTS != 0 -> if (show) SHOW_WITHOUT_POSTS else SHOW_ALL
            else -> throw AssertionError("value $value not supported")
        }
    }

    companion object {
        private val dayRange = 0..30

        const val SHOW_ALL = 1
        const val SHOW_IGNORED = 1 shl 1
        const val SHOW_IN_SAME_DAY = 1 shl 2
        const val SHOW_MISSING = 1 shl 3
        const val SHOW_WITHOUT_POSTS = 1 shl 4
    }
}
