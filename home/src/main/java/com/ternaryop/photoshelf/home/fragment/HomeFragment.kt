package com.ternaryop.photoshelf.home.fragment

import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.fragment.AbsPhotoShelfFragment
import com.ternaryop.photoshelf.home.R
import com.ternaryop.photoshelf.home.prefs.lastStatsRefresh
import com.ternaryop.photoshelf.home.prefs.loadStats
import com.ternaryop.photoshelf.home.prefs.saveStats
import com.ternaryop.photoshelf.lifecycle.EventObserver
import com.ternaryop.photoshelf.lifecycle.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@AndroidEntryPoint
class HomeFragment : AbsPhotoShelfFragment() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLogButton(view)

        viewModel.result.observe(viewLifecycleOwner, EventObserver { result ->
            when (result) {
                is HomeModelResult.Stats -> onStats(result)
            }
        })
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
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val lastRefresh = preferences.lastStatsRefresh
        if (System.currentTimeMillis() - lastRefresh < STATS_REFRESH_RATE_MILLIS) {
            fillStatsUI(preferences.loadStats(viewIdColumnMap))
            return
        }
        viewModel.loadStats(currentBlog)
    }

    private fun onStats(result: HomeModelResult.Stats) {
        when (result.command.status) {
            Status.SUCCESS -> {
                result.command.data?.stats?.also { statsMap ->
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit()
                        .saveStats(statsMap)
                        .lastStatsRefresh(System.currentTimeMillis())
                        .apply()
                    fillStatsUI(statsMap)
                }
            }
            Status.ERROR -> {
            }
            Status.PROGRESS -> {
            }
        }
    }

    private fun setupLogButton(view: View) {
        val logButton = view.findViewById<View>(R.id.open_crash_log)
        val context = requireContext()
        if (CrashLogViewer.file(context).exists()) {
            logButton.visibility = View.VISIBLE
            logButton.setOnClickListener { launch { CrashLogViewer.startView(context) } }
        } else {
            logButton.visibility = View.GONE
        }
    }

    companion object {
        private const val POST_COUNT_COLUMN = "postCount"
        private const val UNIQUE_TAGS_COUNT_COLUMN = "uniqueTagsCount"
        private const val UNIQUE_FIRST_TAG_COUNT_COLUMN = "uniqueFirstTagCount"
        private const val MISSING_BIRTHDAYS_COUNT_COLUMN = "missingBirthdaysCount"
        private const val BIRTHDAYS_COUNT_COLUMN = "birthdaysCount"
        private const val RECORD_COUNT_COLUMN = "recordCount"

        private val viewIdColumnMap = SparseArray<String>().apply {
            put(R.id.total_records, RECORD_COUNT_COLUMN)
            put(R.id.total_posts, POST_COUNT_COLUMN)
            put(R.id.total_unique_tags, UNIQUE_TAGS_COUNT_COLUMN)
            put(R.id.total_unique_first_tags, UNIQUE_FIRST_TAG_COUNT_COLUMN)
            put(R.id.birthdays_count, BIRTHDAYS_COUNT_COLUMN)
            put(R.id.missing_birthdays_count, MISSING_BIRTHDAYS_COUNT_COLUMN)
        }
        private const val STATS_REFRESH_RATE_MILLIS = 6 * 60 * 60 * 1000
    }
}
