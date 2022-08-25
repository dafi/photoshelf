package com.ternaryop.photoshelf.tumblr.ui.core.fragment

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ternaryop.photoshelf.tumblr.ui.core.R
import java.util.Timer
import java.util.TimerTask

private const val UI_DELAY = 20L

fun RecyclerView.areAllItemsVisible(): Boolean {
    val adapter = adapter ?: return true
    val layoutManager = layoutManager

    if (layoutManager is LinearLayoutManager) {
        val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
        val lastVisiblePos = layoutManager.findLastVisibleItemPosition()

        return firstVisiblePos == 0 && lastVisiblePos == adapter.itemCount - 1
    }
    return false
}

class QuickListNavigatorScrollListener(
    private val view: View,
    private val visibilityDuration: Long
) : RecyclerView.OnScrollListener(), View.OnClickListener {
    private var timer: Timer? = null
    private lateinit var recyclerView: RecyclerView

    init {
        view.findViewById<View>(R.id.move_to_top).setOnClickListener(this)
        view.findViewById<View>(R.id.move_to_bottom).setOnClickListener(this)
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (recyclerView.areAllItemsVisible()) {
            return
        }
        this.recyclerView = recyclerView

        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            timer?.cancel()
            timer = Timer(false).also { it.schedule(HideViewTimerTask(), visibilityDuration) }
        } else {
            view.visibility = View.VISIBLE
        }
    }

    private inner class HideViewTimerTask : TimerTask() {
        override fun run() {
            view.postDelayed({ view.visibility = View.GONE }, UI_DELAY)
        }
    }

    override fun onClick(v: View?) {
        val id = v?.id ?: return
        val adapter = recyclerView.adapter ?: return
        val position = when (id) {
            R.id.move_to_top -> 0
            R.id.move_to_bottom -> adapter.itemCount - 1
            else -> return
        }
        recyclerView.postDelayed({
            recyclerView.scrollToPosition(position)
            view.visibility = View.INVISIBLE
        }, UI_DELAY)
    }
}
