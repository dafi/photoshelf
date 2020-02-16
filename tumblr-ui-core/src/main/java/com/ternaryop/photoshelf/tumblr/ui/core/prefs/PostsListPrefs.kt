package com.ternaryop.photoshelf.tumblr.ui.core.prefs

import android.content.SharedPreferences

const val PREF_THUMBNAIL_WIDTH = "thumbnail_width"

fun SharedPreferences.thumbnailWidth(defaultValue: Int): Int =
    getString(PREF_THUMBNAIL_WIDTH, null)?.toInt() ?: defaultValue
