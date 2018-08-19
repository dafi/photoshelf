package com.ternaryop.photoshelf.util.mru

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import java.util.Locale

/**
 * Created by dave on 30/11/17.
 * Handle Most Recently Used (MRU) list
 */

private const val ITEM_SEPARATOR = "\n"

class MRU(context: Context, private val key: String, private val maxSize: Int) {
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val _list: MutableList<String> by lazy {
        preferences.getString(key, null)?.split(ITEM_SEPARATOR)?.mapTo(mutableListOf()) { it }
            ?: mutableListOf<String>()
    }

    val list: List<String> get() = _list

    fun add(items: List<String>) {
        // every item is added at top so iterate in reverse order
        for (item in items.asReversed()) {
            add(item)
        }
    }

    fun add(item: String): Boolean {
        if (item.trim { it <= ' ' }.isEmpty()) {
            return false
        }
        val lowerCaseItem = item.toLowerCase(Locale.US)
        if (!_list.remove(lowerCaseItem) && list.size == maxSize) {
            _list.removeAt(_list.size - 1)
        }
        _list.add(0, lowerCaseItem)

        return true
    }

    fun remove(item: String): Boolean {
        return _list.remove(item.toLowerCase(Locale.US))
    }

    fun save() {
        preferences
                .edit()
                .putString(key, _list.joinToString(ITEM_SEPARATOR))
                .apply()
    }
}
