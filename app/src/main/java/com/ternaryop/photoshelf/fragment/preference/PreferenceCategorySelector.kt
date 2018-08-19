package com.ternaryop.photoshelf.fragment.preference

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.ternaryop.photoshelf.R

private const val CATEGORY_KEY_IMPORT = "import_category_key"

/**
 * Created by dave on 17/03/18.
 * Select preference by category key
 */
object PreferenceCategorySelector {
    fun openScreen(caller: PreferenceFragmentCompat?, pref: PreferenceScreen?) {
        pref ?: return
        val fm = caller?.activity?.supportFragmentManager ?: return

        val fragment = fragmentFromCategory(pref.key) ?: return
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        fragment.arguments = args
        fm.beginTransaction()
            .add(R.id.content_frame, fragment, pref.key)
            .addToBackStack(pref.key)
            .commit()
    }

    private fun fragmentFromCategory(key: String?): Fragment? = when (key) {
        CATEGORY_KEY_IMPORT -> ImportPreferenceFragment()
        else -> null
    }
}