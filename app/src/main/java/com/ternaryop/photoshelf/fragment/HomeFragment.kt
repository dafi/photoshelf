package com.ternaryop.photoshelf.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.PostTagDAO
import java.lang.ref.WeakReference
import java.text.DecimalFormat

class HomeFragment : AbsPhotoShelfFragment() {

    private lateinit var handler: Handler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        refresh()

        handler = UIFillerHandler(this)

        return rootView
    }

    private fun fillStatsUI(statsMap: Map<String, Long>) {
        view?.also {
            val format = DecimalFormat("###,###")
            val containerView = it.findViewById<View>(R.id.home_container)
            containerView.visibility = View.VISIBLE
            for (i in 0 until viewIdColumnMap.size()) {
                val textView = it.findViewById<View>(viewIdColumnMap.keyAt(i)) as TextView
                val count = statsMap[viewIdColumnMap.valueAt(i)]
                textView.text = format.format(count)
                textView.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fade))
            }
        }
    }

    private fun refresh() {
        Thread(Runnable {
            val statsMap = DBHelper.getInstance(activity!!).postTagDAO.getStatisticCounts(blogName)
            handler.obtainMessage(STATS_DATA_OK, statsMap).sendToTarget()
        }).start()
    }

    private class UIFillerHandler internal constructor(homeFragment: HomeFragment) : Handler(Looper.getMainLooper()) {
        private val homeFragment: WeakReference<HomeFragment> = WeakReference(homeFragment)

        override fun handleMessage(msg: Message) {
            val homeFragment = this.homeFragment.get()
            if (msg.what == STATS_DATA_OK && homeFragment != null) {
                @Suppress("UNCHECKED_CAST")
                homeFragment.fillStatsUI(msg.obj as Map<String, Long>)
            }
        }
    }

    companion object {
        private val viewIdColumnMap = SparseArray<String>()

        init {
            viewIdColumnMap.put(R.id.total_records, PostTagDAO.RECORD_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.total_posts, PostTagDAO.POST_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.total_unique_tags, PostTagDAO.UNIQUE_TAGS_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.total_unique_first_tags, PostTagDAO.UNIQUE_FIRST_TAG_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.birthdays_count, PostTagDAO.BIRTHDAYS_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.missing_birthdays_count, PostTagDAO.MISSING_BIRTHDAYS_COUNT_COLUMN)
        }

        private const val STATS_DATA_OK = 1
    }
}
