@file:Suppress("unused")

package com.ternaryop.photoshelf.feedly.prefs

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ternaryop.photoshelf.feedly.R
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs.Companion.PREF_MAX_FETCH_ITEM_COUNT
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs.Companion.PREF_NEWER_THAN_HOURS
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs.Companion.PREF_PICKER_FETCH_ITEM_COUNT

class FeedlyPreferenceFragment
    : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.feedly_settings, rootKey)
        arrayOf(
            PREF_MAX_FETCH_ITEM_COUNT,
            PREF_NEWER_THAN_HOURS,
            PREF_PICKER_FETCH_ITEM_COUNT,
        ).forEach { onSharedPreferenceChanged(preferenceManager.sharedPreferences, it) }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PREF_MAX_FETCH_ITEM_COUNT,
            PREF_NEWER_THAN_HOURS,
            PREF_PICKER_FETCH_ITEM_COUNT ->
                findPreference<Preference>(key)?.summary =
                    sharedPreferences.getInt(key, 0).toString()
        }

    }
}