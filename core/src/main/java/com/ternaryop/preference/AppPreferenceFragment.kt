package com.ternaryop.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat

/**
 * Created by dave on 17/03/18.
 * Base class for preference fragments
 * See {@link https://github.com/madlymad/PreferenceApp}
 */
abstract class AppPreferenceFragment :
    PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set the default white background in the view so as to avoid transparency
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }
}
