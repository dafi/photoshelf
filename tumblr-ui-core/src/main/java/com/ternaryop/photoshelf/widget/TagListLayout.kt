package com.ternaryop.photoshelf.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.updateMargins
import com.google.android.flexbox.FlexboxLayout
import com.ternaryop.photoshelf.tumblr.ui.core.R
import java.util.Locale

class TagListLayout : FlexboxLayout {
    var tagMarginBottom = 0

    @LayoutRes
    var tagLayout: Int = -1
        set(value) {
            check(value > 0) { "TagLayout is mandatory" }
            field = value
        }

    @Dimension
    var tagTextSize = 0f
        set(value) {
            field = value
            updateTagTextSize(value)
        }

    /**
     * Used to determine the clicked tag
     */
    @IdRes
    var tagTextViewId = -1
        set(value) {
            check(value > 0) { "TagTextViewId is mandatory $value ${R.id.tag_text_view}" }
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
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.com_ternaryop_photoshelf_widget_TagListLayout,
            0,
            0
        )
        try {
            tagLayout = a.getResourceId(R.styleable.com_ternaryop_photoshelf_widget_TagListLayout_tagLayout, -1)
            tagTextSize = a.getDimensionPixelSize(R.styleable.com_ternaryop_photoshelf_widget_TagListLayout_tagTextSize, 0).toFloat()
            tagTextViewId = a.getResourceId(R.styleable.com_ternaryop_photoshelf_widget_TagListLayout_tagTextViewId, -1)
            tagMarginBottom = a.getDimensionPixelSize(R.styleable.com_ternaryop_photoshelf_widget_TagListLayout_tagMarginBottom, 0)
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
                val view = LayoutInflater.from(context).inflate(tagLayout, null)

                addView(view)
                // The margin values set on tagLayout are removed by Android so we need to set them programmatically
                // https://github.com/google/flexbox-layout/issues/493#issuecomment-680086563
                (view.layoutParams as? MarginLayoutParams)?.updateMargins(bottom = tagMarginBottom)
            }
            if (tagTextSize > 0) {
                updateTagTextSize((tagTextSize))
            }
        }
        for (i in tags.indices) {
            val tag = tags[i]
            val view = getChildAt(i) as TextView
            view.id = tagTextViewId
            view.text = String.format(Locale.US, "#%s", tag)
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

    private fun updateTagTextSize(value: Float) {
        for (i in 0 until childCount) {
            val view = getChildAt(i) as TextView
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, value)
        }
    }
}
