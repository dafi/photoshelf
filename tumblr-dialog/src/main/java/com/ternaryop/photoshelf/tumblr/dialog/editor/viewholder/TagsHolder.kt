package com.ternaryop.photoshelf.tumblr.dialog.editor.viewholder

import android.content.Context
import android.graphics.Color
import android.widget.MultiAutoCompleteTextView
import com.ternaryop.photoshelf.misspelled.MisspelledName
import com.ternaryop.photoshelf.mru.adapter.OnMRUListener
import com.ternaryop.photoshelf.tagnavigator.adapter.TagNavigatorArrayAdapter
import com.ternaryop.photoshelf.tagnavigator.adapter.TagNavigatorFilter
import com.ternaryop.tumblr.TumblrPost

class TagsHolder(
    context: Context,
    private val textView: MultiAutoCompleteTextView,
    blogName: String
) : OnMRUListener {

    private var defaultColor = textView.textColors
    private var defaultBackground = textView.background

    private val tagAdapter = TagNavigatorArrayAdapter(
        context,
        com.ternaryop.photoshelf.tagnavigator.R.layout.tag_navigator_row,
        blogName
    )

    init {
        textView.setAdapter(tagAdapter)
        textView.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
    }

    fun updateBlogName(blogName: String) {
        (tagAdapter.filter as TagNavigatorFilter).blogName = blogName
        tagAdapter.notifyDataSetChanged()
    }

    var tags: String
        get() {
            return cleanSeparators(textView.text)
        }
        set(value) = textView.setText(value)

    fun highlightTagName(misspelledInfo: MisspelledName.Info) {
        when (misspelledInfo) {
            is MisspelledName.Info.AlreadyExists -> {
                textView.setTextColor(defaultColor)
                textView.background = defaultBackground
            }
            is MisspelledName.Info.Corrected -> {
                textView.setTextColor(Color.RED)
                textView.setBackgroundColor(Color.YELLOW)
                val replaced = toggleTag(misspelledInfo.misspelledName).apply {
                    add(0, misspelledInfo.name)
                }.joinToString(", ")
                textView.setText(replaced)
            }
            is MisspelledName.Info.NotFound -> {
                textView.setTextColor(Color.WHITE)
                textView.setBackgroundColor(Color.RED)
            }
        }
    }

    override fun onItemSelect(item: String) {
        textView.setText(toggleTag(item).joinToString(", "))
        textView.moveCaretToEnd()
        textView.requestFocus()
    }

    private fun toggleTag(item: String): MutableList<String> {
        val tags = TumblrPost.tagsFromString(tags)
        val index = tags.indexOfFirst { it.equals(item, true) }

        if (index < 0) {
            tags.add(item)
        } else {
            tags.removeAt(index)
        }
        return tags
    }

    override fun onItemDelete(item: String) = Unit

    companion object {
        fun cleanSeparators(text: CharSequence, sep: String = ","): String =
            text.replace("$sep+".toRegex(), sep).replace("$sep\\s*$".toRegex(), "")
    }
}
