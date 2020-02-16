package com.ternaryop.photoshelf.core.prefs

import android.content.SharedPreferences

const val PREF_PHOTOSHELF_APIKEY = "photoshelfApikey"

val SharedPreferences.photoShelfApikey: String
    get() = getString(PREF_PHOTOSHELF_APIKEY, null) ?: ""
