package com.ternaryop.photoshelf.core.prefs

import android.content.SharedPreferences

const val PREF_KEY_SHOW_PICKER_SHARE_MENU = "show_picker_share_menu"

val SharedPreferences.showPickerShareMenu: Boolean
    get() = getBoolean(PREF_KEY_SHOW_PICKER_SHARE_MENU, true)
