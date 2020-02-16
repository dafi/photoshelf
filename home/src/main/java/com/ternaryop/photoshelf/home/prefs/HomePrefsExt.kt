package com.ternaryop.photoshelf.home.prefs

import android.content.SharedPreferences
import android.util.SparseArray

private const val PREF_LAST_STATS_REFRESH = "home_last_stat_refresh"
private const val PREF_HOME_STATS_PREFIX = "home_stats:"

internal fun SharedPreferences.loadStats(viewIdColumnMap: SparseArray<String>): Map<String, Long> {
    val map = mutableMapOf<String, Long>()

    for (i in 0 until viewIdColumnMap.size()) {
        val k = viewIdColumnMap.valueAt(i)
        map[k] = getLong(PREF_HOME_STATS_PREFIX + k, -1)
    }
    return map
}

internal fun SharedPreferences.Editor.saveStats(statsMap: Map<String, Long>): SharedPreferences.Editor {
    for ((k, v) in statsMap) {
        putLong(PREF_HOME_STATS_PREFIX + k, v)
    }
    return this
}

internal val SharedPreferences.lastStatsRefresh: Long
    get() = getLong(PREF_LAST_STATS_REFRESH, 0)

internal fun SharedPreferences.Editor.lastStatsRefresh(refresh: Long): SharedPreferences.Editor {
    return putLong(PREF_LAST_STATS_REFRESH, refresh)
}
