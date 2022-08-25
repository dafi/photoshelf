package com.ternaryop.photoshelf.tumblr.ui.core.adapter

import android.content.SharedPreferences

enum class ViewType {
    List,
    Grid;

    fun save(editor: SharedPreferences.Editor, prefName: String): SharedPreferences.Editor =
        editor.putInt(prefName, ordinal)

    companion object {
        fun load(
            preferences: SharedPreferences,
            prefName: String,
            defaultValue: ViewType = List
        ) = values()[preferences.getInt(prefName, defaultValue.ordinal)]
    }
}
