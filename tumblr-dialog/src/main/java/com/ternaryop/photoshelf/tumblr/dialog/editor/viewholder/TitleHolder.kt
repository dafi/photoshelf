package com.ternaryop.photoshelf.tumblr.dialog.editor.viewholder

import android.widget.EditText
import com.ternaryop.utils.text.fromHtml
import com.ternaryop.utils.text.toHtml

fun EditText.moveCaretToEnd() = setSelection(length())

class TitleHolder(
    private val editText: EditText,
    private var sourceTitle: String,
    htmlSourceTitle: String
) {

    var htmlTitle: String
        get() {
            editText.clearComposingText()
            return editText.text.toHtml()
        }
        set(value) {
            editText.setText(value.fromHtml())
            editText.moveCaretToEnd()
        }

    val plainTitle: String
        get() {
            return editText.text.toString()
        }

    init {
        htmlTitle = htmlSourceTitle
    }

    fun restoreSourceTitle() {
        htmlTitle = sourceTitle
    }
}
