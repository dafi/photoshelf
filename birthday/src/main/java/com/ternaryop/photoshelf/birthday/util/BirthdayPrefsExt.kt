package com.ternaryop.photoshelf.birthday.util

import android.content.SharedPreferences

private const val LAST_BIRTHDAY_SHOW_TIME = "lastBirthdayShowTime"

var SharedPreferences.lastBirthdayShowTime: Long
    get() = getLong(LAST_BIRTHDAY_SHOW_TIME, 0)
    set(timems) = edit().putLong(LAST_BIRTHDAY_SHOW_TIME, timems).apply()
