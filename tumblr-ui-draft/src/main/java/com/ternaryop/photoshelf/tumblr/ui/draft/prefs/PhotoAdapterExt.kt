package com.ternaryop.photoshelf.tumblr.ui.draft.prefs

import android.content.SharedPreferences
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo.PhotoAdapter
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo.PhotoSortSwitcher

private const val PREF_DRAFT_SORT_TYPE = "draft_sort_type"
private const val PREF_DRAFT_SORT_ASCENDING = "draft_sort_ascending"

internal fun PhotoAdapter.loadSortSettings(preferences: SharedPreferences) {
    sortSwitcher.setType(
        preferences.getInt(PREF_DRAFT_SORT_TYPE, PhotoSortSwitcher.LAST_PUBLISHED_TAG),
        preferences.getBoolean(PREF_DRAFT_SORT_ASCENDING, true))
}

internal fun PhotoAdapter.saveSortSettings(editor: SharedPreferences.Editor): SharedPreferences.Editor =
    editor
        .putInt(PREF_DRAFT_SORT_TYPE, sortSwitcher.sortable.sortId)
        .putBoolean(PREF_DRAFT_SORT_ASCENDING, sortSwitcher.sortable.isAscending)
