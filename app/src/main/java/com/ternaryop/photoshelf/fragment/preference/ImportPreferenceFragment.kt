package com.ternaryop.photoshelf.fragment.preference

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.core.prefs.PREF_EXPORT_DAYS_PERIOD
import com.ternaryop.photoshelf.core.prefs.exportDaysPeriod
import com.ternaryop.photoshelf.core.prefs.lastFollowersUpdateTime
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.domselector.DomSelectorManager
import com.ternaryop.photoshelf.service.ImportService
import com.ternaryop.utils.date.daysSinceNow
import com.ternaryop.utils.dialog.showErrorDialog

private const val KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA = "import_birthdays_from_wikipedia"
private const val KEY_IMPORT_DOM_SELECTOR_CONFIG = "import_dom_selector_config"
private const val DOM_SELECTOR_CONFIG_PICKED_REQUEST_CODE = 1

/**
 * Created by dave on 17/03/18.
 * Hold Import/Export Preferences
 */
class ImportPreferenceFragment : AppPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        with(preferenceScreen) {
            findPreference<Preference>(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA)
                ?.isEnabled = !sharedPreferences.selectedBlogName.isNullOrEmpty()

            onSharedPreferenceChanged(preferenceManager.sharedPreferences, PREF_EXPORT_DAYS_PERIOD)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA -> {
                preferenceScreen.sharedPreferences.selectedBlogName?.also {
                    ImportService.startImportBirthdaysFromWeb(requireContext(), it)
                }
                true
            }
            KEY_IMPORT_DOM_SELECTOR_CONFIG -> {
                // on Oreo and below "application/json" isn't handled so we must add "application/octet-stream", too
                performPick(
                    "*/*",
                    arrayOf("application/json", "application/octet-stream"),
                    DOM_SELECTOR_CONFIG_PICKED_REQUEST_CODE)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun performPick(mediaType: String, extraMediaTypes: Array<String>, requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mediaType
            putExtra(Intent.EXTRA_MIME_TYPES, extraMediaTypes)
        }

        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == DOM_SELECTOR_CONFIG_PICKED_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                try {
                    DomSelectorManager.upgradeConfig(requireContext(), uri)
                } catch (e: Exception) {
                    e.showErrorDialog(requireContext())
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PREF_EXPORT_DAYS_PERIOD -> onChangedExportDaysPeriod(sharedPreferences, key)
        }
    }

    private fun onChangedExportDaysPeriod(sharedPreferences: SharedPreferences, key: String) {
        val days = sharedPreferences.exportDaysPeriod(requireContext())
        val lastFollowersUpdateTime = sharedPreferences.lastFollowersUpdateTime
        val remainingMessage: String
        remainingMessage = if (lastFollowersUpdateTime < 0) {
            resources.getString(R.string.never_run)
        } else {
            val remainingDays = (days - lastFollowersUpdateTime.daysSinceNow()).toInt()
            resources.getQuantityString(R.plurals.next_in_day, remainingDays, remainingDays)
        }
        findPreference<Preference>(PREF_EXPORT_DAYS_PERIOD)?.summary =
            resources.getQuantityString(R.plurals.day_title, days, days) + " ($remainingMessage)"
    }
}
