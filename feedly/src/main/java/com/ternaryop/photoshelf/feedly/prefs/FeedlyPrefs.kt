@file:Suppress("MaxLineLength", "ArgumentListWrapping")

package com.ternaryop.photoshelf.feedly.prefs

import android.content.Context
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentSortSwitcher

class FeedlyPrefs(private val context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val newerThanHours: Int
        get() = preferences.getInt(PREF_NEWER_THAN_HOURS, context.resources.getInteger(R.integer.newer_than_hours_default))
    val maxFetchItemCount: Int
        get() = preferences.getInt(PREF_MAX_FETCH_ITEM_COUNT, context.resources.getInteger(R.integer.max_fetch_items_count_default))
    val deleteOnRefresh: Boolean
        get() = preferences.getBoolean(PREF_DELETE_ON_REFRESH, context.resources.getBoolean(R.bool.delete_on_refresh_default))
    val pickerFetchItemCount: Int
        get() = preferences.getInt(PREF_PICKER_FETCH_ITEM_COUNT, context.resources.getInteger(R.integer.picker_fetch_items_count_default))

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

    companion object {
        const val PREF_MAX_FETCH_ITEM_COUNT = "feedly.MaxFetchItemCount"
        const val PREF_PICKER_FETCH_ITEM_COUNT = "feedly.PickerFetchItemCount"
        const val PREF_NEWER_THAN_HOURS = "feedly.NewerThanHours"
        const val PREF_DELETE_ON_REFRESH = "feedly.DeleteOnRefresh"
        const val PREF_SORT_TYPE = "feedly.SortType"
        const val PREF_SORT_ASCENDING = "feedly.SortAscending"
        const val PREF_SELECTED_CATEGORIES = "feedly.SelectedCategories"
        const val PREF_FEEDLY_ACCESS_TOKEN = "feedly.AccessToken"
    }
}
