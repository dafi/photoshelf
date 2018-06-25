package com.ternaryop.photoshelf.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.PreferenceManager
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.util.network.ApiManager
import java.lang.ref.WeakReference
import java.text.DecimalFormat

class HomeFragment : AbsPhotoShelfFragment() {

    private lateinit var handler: Handler

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        handler = UIFillerHandler(this)

        refresh()

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
        val preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        val lastRefresh = preferences.getLong(PREF_LAST_STATS_REFRESH, System.currentTimeMillis())
        if (System.currentTimeMillis() - lastRefresh < STATS_REFRESH_RATE_MILLIS) {
            handler.obtainMessage(STATS_DATA_OK, loadStats(preferences)).sendToTarget()
            return
        }
        preferences.edit().putLong(PREF_LAST_STATS_REFRESH, System.currentTimeMillis()).apply()
        Thread(Runnable {
            try {
                val statsMap = ApiManager.postManager(activity!!).getStats(blogName!!)
                saveStats(preferences, statsMap)
                handler.obtainMessage(STATS_DATA_OK, statsMap).sendToTarget()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    private fun loadStats(preferences: SharedPreferences): Map<String, Long> {
        val map = mutableMapOf<String, Long>()

        for (i in 0 until viewIdColumnMap.size()) {
            val k = viewIdColumnMap.valueAt(i)
            map[k] = preferences.getLong(PREF_HOME_STATS_PREFIX + k, -1)
        }
        return map
    }

    private fun saveStats(preferences: SharedPreferences, statsMap: Map<String, Long>) {
        val editor = preferences.edit()
        for ((k, v) in statsMap) {
            editor.putLong(PREF_HOME_STATS_PREFIX + k, v)
        }
        editor.apply()
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
        private const val POST_COUNT_COLUMN = "postCount"
        private const val UNIQUE_TAGS_COUNT_COLUMN = "uniqueTagsCount"
        private const val UNIQUE_FIRST_TAG_COUNT_COLUMN = "uniqueFirstTagCount"
        private const val MISSING_BIRTHDAYS_COUNT_COLUMN = "missingBirthdaysCount"
        private const val BIRTHDAYS_COUNT_COLUMN = "birthdaysCount"
        private const val RECORD_COUNT_COLUMN = "recordCount"

        private val viewIdColumnMap = SparseArray<String>()

        init {
            viewIdColumnMap.put(R.id.total_records, RECORD_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.total_posts, POST_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.total_unique_tags, UNIQUE_TAGS_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.total_unique_first_tags, UNIQUE_FIRST_TAG_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.birthdays_count, BIRTHDAYS_COUNT_COLUMN)
            viewIdColumnMap.put(R.id.missing_birthdays_count, MISSING_BIRTHDAYS_COUNT_COLUMN)
        }

        private const val STATS_DATA_OK = 1
        private const val PREF_LAST_STATS_REFRESH = "home_last_stat_refresh"
        private const val PREF_HOME_STATS_PREFIX = "home_stats:"
        private const val STATS_REFRESH_RATE_MILLIS = 6 * 60 * 60 * 1000
    }
}
