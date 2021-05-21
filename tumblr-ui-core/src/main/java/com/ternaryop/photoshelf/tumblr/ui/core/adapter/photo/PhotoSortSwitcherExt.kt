package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.content.SharedPreferences

private const val PREF_SORT_TYPE = "sort_type"
private const val PREF_SORT_ASCENDING = "sort_ascending"

fun PhotoSortSwitcher.loadSettings(
    prefNamePrefix: String,
    preferences: SharedPreferences
) {
    setType(
        preferences.getInt(prefNamePrefix + PREF_SORT_TYPE, PhotoSortSwitcher.LAST_PUBLISHED_TAG),
        preferences.getBoolean(prefNamePrefix + PREF_SORT_ASCENDING, true)
    )
}

fun PhotoSortSwitcher.saveSettings(
    prefNamePrefix: String,
    editor: SharedPreferences.Editor
): SharedPreferences.Editor =
    editor
        .putInt(prefNamePrefix + PREF_SORT_TYPE, sortable.sortId)
        .putBoolean(prefNamePrefix + PREF_SORT_ASCENDING, sortable.isAscending)
