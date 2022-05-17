package com.ternaryop.photoshelf.birthday.util

import android.content.SharedPreferences

private const val LAST_BIRTHDAY_SHOW_TIME = "lastBirthdayShowTime"
private const val SHOW_BIRTHDAYS_NOTIFICATION = "show_birthdays_notification"

var SharedPreferences.lastBirthdayShowTime: Long
    get() = getLong(LAST_BIRTHDAY_SHOW_TIME, 0)
    set(timeMS) = edit().putLong(LAST_BIRTHDAY_SHOW_TIME, timeMS).apply()

var SharedPreferences.showBirthdaysNotification: Boolean
    get() = getBoolean(SHOW_BIRTHDAYS_NOTIFICATION, true)
    set(show) = edit().putBoolean(SHOW_BIRTHDAYS_NOTIFICATION, show).apply()
