package com.ternaryop.photoshelf.fragment.preference

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ternaryop.photoshelf.R

private const val CATEGORY_KEY_IMPORT = "category_key_import"
private const val CATEGORY_KEY_IMAGE = "category_key_image"

/**
 * Created by dave on 17/03/18.
 * Select preference by category key
 */
object PreferenceCategorySelector {
    fun openScreen(
        caller: PreferenceFragmentCompat,
        pref: Preference,
        fragment: Fragment
    ): Boolean {
        val fm = caller.activity?.supportFragmentManager ?: return false

        val args = fragment.arguments ?: Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        fragment.arguments = args
        fm.beginTransaction()
            .add(R.id.content_frame, fragment, pref.key)
            .addToBackStack(pref.key)
            .commit()
        return true
    }

    fun fragmentFromCategory(key: String?): Fragment? = when (key) {
        CATEGORY_KEY_IMPORT -> ImportPreferenceFragment()
        CATEGORY_KEY_IMAGE -> ImagePreferenceFragment()
        else -> null
    }
}
