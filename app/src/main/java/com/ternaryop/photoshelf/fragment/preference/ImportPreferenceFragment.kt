package com.ternaryop.photoshelf.fragment.preference

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.core.prefs.PREF_EXPORT_DAYS_PERIOD
import com.ternaryop.photoshelf.core.prefs.exportDaysPeriod
import com.ternaryop.photoshelf.core.prefs.lastFollowersUpdateTime
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.domselector.DomSelectorManager
import com.ternaryop.photoshelf.service.ImportService
import com.ternaryop.preference.AppPreferenceFragment
import com.ternaryop.utils.date.daysSinceNow
import com.ternaryop.utils.dialog.showErrorDialog

private const val KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA = "import_birthdays_from_wikipedia"
private const val KEY_IMPORT_DOM_SELECTOR_CONFIG = "import_dom_selector_config"

/**
 * Created by dave on 17/03/18.
 * Hold Import/Export Preferences
 */
class ImportPreferenceFragment : AppPreferenceFragment() {
    private lateinit var activityResult: ActivityResultLauncher<Array<String>>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityResult = registerForActivityResult(object : ActivityResultContracts.OpenDocument() {
            override fun createIntent(context: Context, input: Array<out String>)
                = super.createIntent(context, input).apply { addCategory(Intent.CATEGORY_OPENABLE) }
        }) { uri ->
            try {
                uri?.also {
                    DomSelectorManager.upgradeConfig(requireContext(), uri)
                }
            } catch (e: Exception) {
                e.showErrorDialog(requireContext())
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        with(preferenceScreen) {
            findPreference<Preference>(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA)
                ?.isEnabled = !sharedPreferences?.selectedBlogName.isNullOrEmpty()

            onSharedPreferenceChanged(preferenceManager.sharedPreferences, PREF_EXPORT_DAYS_PERIOD)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA -> {
                preferenceScreen.sharedPreferences?.selectedBlogName?.also {
                    ImportService.startImportBirthdaysFromWeb(requireContext(), it)
                }
                true
            }
            KEY_IMPORT_DOM_SELECTOR_CONFIG -> {
                // on Oreo and below "application/json" isn't handled so we must add "application/octet-stream", too
                activityResult.launch(arrayOf("application/json", "application/octet-stream"))
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PREF_EXPORT_DAYS_PERIOD -> onChangedExportDaysPeriod(sharedPreferences, key)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onChangedExportDaysPeriod(sharedPreferences: SharedPreferences, key: String) {
        val days = sharedPreferences.exportDaysPeriod(requireContext())
        val lastFollowersUpdateTime = sharedPreferences.lastFollowersUpdateTime
        val remainingMessage = if (lastFollowersUpdateTime < 0) {
            resources.getString(R.string.never_run)
        } else {
            val remainingDays = (days - lastFollowersUpdateTime.daysSinceNow()).toInt()
            resources.getQuantityString(R.plurals.next_in_day, remainingDays, remainingDays)
        }
        findPreference<Preference>(PREF_EXPORT_DAYS_PERIOD)?.summary =
            resources.getQuantityString(R.plurals.day_title, days, days) + " ($remainingMessage)"
    }
}
