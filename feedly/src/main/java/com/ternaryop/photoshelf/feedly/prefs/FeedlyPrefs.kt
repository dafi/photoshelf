package com.ternaryop.photoshelf.feedly.prefs

import android.content.Context
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentSortSwitcher

class FeedlyPrefs(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val newerThanHours: Int
        get() = preferences.getInt(PREF_NEWER_THAN_HOURS, DEFAULT_NEWER_THAN_HOURS)
    val maxFetchItemCount: Int
        get() = preferences.getInt(PREF_MAX_FETCH_ITEMS_COUNT, DEFAULT_MAX_FETCH_ITEMS_COUNT)
    val deleteOnRefresh: Boolean
        get() = preferences.getBoolean(PREF_DELETE_ON_REFRESH, true)

    var selectedCategoriesId: Set<String>
        get() = preferences.getStringSet(PREF_SELECTED_CATEGORIES, null) ?: emptySet()
        set(selection) = preferences.edit().putStringSet(PREF_SELECTED_CATEGORIES, selection).apply()

    var accessToken: String?
        get() = preferences.getString(PREF_FEEDLY_ACCESS_TOKEN, null)
        set(token) = preferences.edit().putString(PREF_FEEDLY_ACCESS_TOKEN, token).apply()

    fun getSortType() = preferences.getInt(PREF_SORT_TYPE, FeedlyContentSortSwitcher.TITLE_NAME)

    fun saveSortSettings(sortType: Int, sortAscending: Boolean) {
        preferences
            .edit()
            .putInt(PREF_SORT_TYPE, sortType)
            .putBoolean(PREF_SORT_ASCENDING, sortAscending)
            .apply()
    }

    fun saveOtherSettings(fetchCount: Int, newerThanHours: Int, deleteOnRefresh: Boolean) {
        preferences.edit()
            .putInt(PREF_MAX_FETCH_ITEMS_COUNT, fetchCount)
            .putInt(PREF_NEWER_THAN_HOURS, newerThanHours)
            .putBoolean(PREF_DELETE_ON_REFRESH, deleteOnRefresh)
            .apply()
    }

    companion object {
        const val PREF_MAX_FETCH_ITEMS_COUNT = "feedly.MaxFetchItemCount"
        const val PREF_NEWER_THAN_HOURS = "feedly.NewerThanHours"
        const val PREF_DELETE_ON_REFRESH = "feedly.DeleteOnRefresh"
        const val PREF_SORT_TYPE = "feedly.SortType"
        const val PREF_SORT_ASCENDING = "feedly.SortAscending"
        const val PREF_SELECTED_CATEGORIES = "feedly.SelectedCategories"
        const val PREF_FEEDLY_ACCESS_TOKEN = "feedly.AccessToken"

        const val DEFAULT_MAX_FETCH_ITEMS_COUNT = 300
        const val DEFAULT_NEWER_THAN_HOURS = 24
    }
}
