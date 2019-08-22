package com.ternaryop.photoshelf.view

import android.content.Context
import android.util.AttributeSet
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout

/**
 * Created by dave on 08/09/17.
 *
 * Hold the WaitingResultSwipeRefreshLayout used in many classes
 */

class PhotoShelfSwipe : WaitingResultSwipeRefreshLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setOnRefreshListener(listener: OnRefreshListener?) {
        if (listener == null) {
            isEnabled = false
        }
        super.setOnRefreshListener(listener)
    }
}
