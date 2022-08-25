@file:Suppress("unused")

package com.ternaryop.photoshelf.feedly.prefs

import android.content.SharedPreferences
import android.os.Bundle
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.preference.AppPreferenceFragment

class FeedlyPreferenceFragment :
    AppPreferenceFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.feedly_settings, rootKey)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
    }
}
