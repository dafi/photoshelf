package com.ternaryop.photoshelf.tumblr.ui.draft.fragment

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ternaryop.photoshelf.tumblr.ui.draft.R
import com.ternaryop.widget.ProgressHighlightViewLayout
import com.ternaryop.widget.WaitingResultSwipeRefreshLayout

internal class RefreshHolder(
    val context: Context,
    private val progressHighlightViewLayout: ProgressHighlightViewLayout,
    private val swipeLayout: WaitingResultSwipeRefreshLayout,
    listener: SwipeRefreshLayout.OnRefreshListener
) {
    val isRefreshing: Boolean
        get() = swipeLayout.isWaitingResult

    val progressIndicator: TextView
        get() = progressHighlightViewLayout.currentView as TextView

    init {
        progressHighlightViewLayout.progressAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_loop)
        swipeLayout.setColorScheme(R.array.progress_swipe_colors)
        swipeLayout.setOnRefreshListener(listener)
    }

    fun advanceProgressIndicator() = progressHighlightViewLayout.incrementProgress()

    fun onStarted() {
        progressHighlightViewLayout.visibility = View.VISIBLE
        progressHighlightViewLayout.startProgress()
        progressIndicator.text = context.resources.getString(R.string.start_import_title)
        swipeLayout.setRefreshingAndWaitingResult(true)
    }

    fun onCompleted() {
        swipeLayout.setRefreshingAndWaitingResult(false)
        progressHighlightViewLayout.stopProgress()
        progressHighlightViewLayout.visibility = View.GONE
    }
}
