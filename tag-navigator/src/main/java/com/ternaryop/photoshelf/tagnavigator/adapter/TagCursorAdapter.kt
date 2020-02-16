package com.ternaryop.photoshelf.tagnavigator.adapter

import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.provider.BaseColumns
import android.view.View
import android.widget.TextView
import androidx.cursoradapter.widget.SimpleCursorAdapter
import com.ternaryop.photoshelf.api.post.TagInfo
import com.ternaryop.photoshelf.tagnavigator.R
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.htmlHighlightPattern

/**
 * Used by searchView in actionBar
 * @author dave
 */
class TagCursorAdapter(
    private val context: Context,
    resId: Int,
    var blogName: String
) : SimpleCursorAdapter(context, resId, null, arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1),
    intArrayOf(android.R.id.text1), 0), SimpleCursorAdapter.ViewBinder {

    private var pattern = ""

    init {
        viewBinder = this
    }

    override fun convertToString(cursor: Cursor?): CharSequence {
        return cursor?.run {
            getString(getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_TEXT_1))
        } ?: ""
    }

    override fun setViewValue(view: View, cursor: Cursor, columnIndex: Int): Boolean {
        val countColumnIndex = cursor.getColumnIndexOrThrow(SearchManager.SUGGEST_COLUMN_INTENT_DATA)
        val postCount = cursor.getInt(countColumnIndex)
        (view as TextView).text = when {
            pattern.isEmpty() -> context.getString(R.string.tag_with_post_count,
                cursor.getString(columnIndex), postCount)
            else -> {
                val htmlHighlightPattern = cursor.getString(columnIndex).htmlHighlightPattern(pattern)
                context.getString(R.string.tag_with_post_count, htmlHighlightPattern, postCount).fromHtml()
            }
        }
        return true
    }

    fun createCursor(pattern: String, tagInfoList: List<TagInfo>): Cursor {
        this.pattern = pattern
        val cursor = MatrixCursor(COLUMNS)

        for ((i, data) in tagInfoList.withIndex()) {
            cursor.addRow(arrayOf(i, data.tag, data.postCount))
        }
        return cursor
    }

    companion object {
        private val COLUMNS = arrayOf(
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA)
    }
}
