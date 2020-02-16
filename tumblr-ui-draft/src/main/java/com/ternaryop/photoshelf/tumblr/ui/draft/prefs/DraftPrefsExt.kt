package com.ternaryop.photoshelf.tumblr.ui.draft.prefs

import android.content.Context
import android.content.SharedPreferences
import com.ternaryop.photoshelf.core.R

const val PREF_SCHEDULE_MINUTES_TIME_SPAN = "schedule_minutes_time_span"

fun SharedPreferences.defaultScheduleMinutesTimeSpan(context: Context): Int =
    getInt(PREF_SCHEDULE_MINUTES_TIME_SPAN, context.resources.getInteger(R.integer.schedule_minutes_time_span_default))
