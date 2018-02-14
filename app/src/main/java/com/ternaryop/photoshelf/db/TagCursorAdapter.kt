package com.ternaryop.photoshelf.db

import android.content.Context
import android.database.Cursor
import android.support.v4.widget.SimpleCursorAdapter
import android.view.View
import android.widget.FilterQueryProvider
import android.widget.TextView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.util.text.fromHtml
import com.ternaryop.utils.StringUtils

/**
 * Used by searchView in actionBar
 * @author dave
 */
class TagCursorAdapter(private val context: Context, resId: Int, var blogName: String)
    : SimpleCursorAdapter(context, resId, null, arrayOf(PostTagDAO.TAG), intArrayOf(android.R.id.text1), 0), FilterQueryProvider, SimpleCursorAdapter.ViewBinder {
    private val postTagDAO = DBHelper.getInstance(context).postTagDAO
    private var pattern = ""

    init {
        viewBinder = this
        filterQueryProvider = this
    }

    override fun runQuery(constraint: CharSequence?): Cursor {
        this.pattern = constraint?.toString()?.trim { it <= ' ' } ?: ""
        return postTagDAO.getCursorByTag(pattern, blogName)
    }

    override fun convertToString(cursor: Cursor?): String {
        val columnIndex = cursor!!.getColumnIndexOrThrow(PostTagDAO.TAG)
        return cursor.getString(columnIndex)
    }

    override fun setViewValue(view: View, cursor: Cursor, columnIndex: Int): Boolean {
        val countColumnIndex = cursor.getColumnIndexOrThrow("post_count")
        val postCount = cursor.getInt(countColumnIndex)
        if (pattern.isEmpty()) {
            (view as TextView).text = context.getString(R.string.tag_with_post_count, cursor.getString(columnIndex), postCount)
        } else {
            val htmlHighlightPattern = StringUtils.htmlHighlightPattern(pattern, cursor.getString(columnIndex))
            (view as TextView).text = context.getString(R.string.tag_with_post_count, htmlHighlightPattern, postCount).fromHtml()
        }
        return true
    }
}
