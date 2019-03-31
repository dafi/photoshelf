package com.ternaryop.photoshelf.fragment

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceManager
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.text.DecimalFormat

class HomeFragment : AbsPhotoShelfFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        refresh()
    }

    private fun fillStatsUI(statsMap: Map<String, Long>) {
        view?.also {
            val format = DecimalFormat("###,###")
            val containerView = it.findViewById<View>(R.id.home_container)
            containerView.visibility = View.VISIBLE
            for (i in 0 until viewIdColumnMap.size()) {
                val textView = it.findViewById<TextView>(viewIdColumnMap.keyAt(i))
                val count = statsMap[viewIdColumnMap.valueAt(i)]
                textView.text = format.format(count)
                textView.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.fade))
            }
        }
    }

    private fun refresh() {
        val currentBlog = blogName ?: return
        val preferences = PreferenceManager.getDefaultSharedPreferences(context!!)
        val lastRefresh = preferences.getLong(PREF_LAST_STATS_REFRESH, 0)
        if (System.currentTimeMillis() - lastRefresh < STATS_REFRESH_RATE_MILLIS) {
            fillStatsUI(loadStats(preferences))
            return
        }
        val d = ApiManager.postService().getStats(currentBlog)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                val statsMap = response.response.stats
                saveStats(preferences, statsMap)
                preferences.edit().putLong(PREF_LAST_STATS_REFRESH, System.currentTimeMillis()).apply()
                fillStatsUI(statsMap)
            }, { it.printStackTrace() })
        compositeDisposable.add(d)
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

        private const val PREF_LAST_STATS_REFRESH = "home_last_stat_refresh"
        private const val PREF_HOME_STATS_PREFIX = "home_stats:"
        private const val STATS_REFRESH_RATE_MILLIS = 6 * 60 * 60 * 1000
    }
}
