package com.ternaryop.photoshelf.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.google.android.flexbox.FlexboxLayout
import com.ternaryop.photoshelf.tumblr.ui.core.R

class TagListLayout : FlexboxLayout {
    @LayoutRes
    var tagLayout: Int = -1
        set(value) {
            check(value > 0) { "Tag layout is mandatory"}
            field = value
        }

    constructor(context: Context) : super(context) {
        setup(null)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        setup(attrs)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup(attrs)
    }

    private fun setup(attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(attrs,
            R.styleable.com_ternaryop_photoshelf_widget_TagListLayout, 0, 0)
        try {
            tagLayout = a.getResourceId(R.styleable.com_ternaryop_photoshelf_widget_TagListLayout_tagLayout, -1)
        } finally {
            a.recycle()
        }
    }

    @SuppressLint("InflateParams")
    fun addTags(tags: List<String>) {
        val tagsCount = tags.size
        val viewCount = childCount
        val delta = tagsCount - viewCount

        if (delta < 0) {
            for (i in tagsCount until viewCount) {
                getChildAt(i).visibility = View.GONE
            }
        } else if (delta > 0) {
            for (i in 0 until delta) {
                addView(LayoutInflater.from(context).inflate(tagLayout, null))
            }
        }
        for (i in tags.indices) {
            val tag = tags[i]
            val view = getChildAt(i) as TextView
            view.id = R.id.tag_text_view
            view.text = String.format("#%s", tag)
            view.tag = tag
            view.visibility = View.VISIBLE
        }
    }

    fun setOnTagClickListener(l: OnClickListener?) {
        for (i in 0 until childCount) {
            getChildAt(i).setOnClickListener(l)
        }
    }

    fun setTagTextColor(color: ColorStateList?) {
        for (i in 0 until childCount) {
            val view = getChildAt(i) as TextView
            view.setTextColor(color)
        }
    }
}