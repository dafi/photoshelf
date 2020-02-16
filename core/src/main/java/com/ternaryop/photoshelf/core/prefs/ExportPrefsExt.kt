package com.ternaryop.photoshelf.core.prefs

import android.content.Context
import android.content.SharedPreferences
import com.ternaryop.photoshelf.core.R

const val AUTOMATIC_EXPORT = "automatic_export"
const val PREF_EXPORT_DAYS_PERIOD = "exportDaysPeriod"
const val PREF_LAST_FOLLOWERS_UPDATE_TIME = "lastFollowersUpdateTime"

val SharedPreferences.isAutomaticExportEnabled: Boolean
    get() = getBoolean(AUTOMATIC_EXPORT, false)

fun SharedPreferences.exportDaysPeriod(context: Context): Int =
    getInt(PREF_EXPORT_DAYS_PERIOD, context.resources.getInteger(R.integer.export_days_period_default))

var SharedPreferences.lastFollowersUpdateTime: Long
    get() = getLong(PREF_LAST_FOLLOWERS_UPDATE_TIME, -1)
    set(millisecs) = edit().putLong(PREF_LAST_FOLLOWERS_UPDATE_TIME, millisecs).apply()
