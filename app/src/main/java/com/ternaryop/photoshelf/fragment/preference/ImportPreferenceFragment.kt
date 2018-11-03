package com.ternaryop.photoshelf.fragment.preference

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.service.ImportIntentService
import com.ternaryop.utils.date.daysSinceNow
import java.io.File

private const val KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA = "import_birthdays_from_wikipedia"

/**
 * Created by dave on 17/03/18.
 * Hold Import/Export Preferences
 */
class ImportPreferenceFragment : AppPreferenceFragment() {
    private lateinit var appSupport: AppSupport
    private val importer: Importer
        get() = Importer(context!!)

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        appSupport = AppSupport(context!!)

        with(preferenceScreen) {
            findPreference(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA)

            findPreference(AppSupport.PREF_EXPORT_DAYS_PERIOD)
            onSharedPreferenceChanged(preferenceManager.sharedPreferences, AppSupport.PREF_EXPORT_DAYS_PERIOD)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        return when (preference?.key) {
            KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA -> {
                ImportIntentService.startImportBirthdaysFromWeb(context!!, appSupport.selectedBlogName!!)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun setupPreferenceFilePath(fullPath: String, prefKey: String,
        preferenceScreen: PreferenceScreen): Preference {
        val pref = preferenceScreen.findPreference(prefKey)
        pref.summary = fullPath
        pref.isEnabled = File(fullPath).exists()
        return pref
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            AppSupport.PREF_EXPORT_DAYS_PERIOD -> onChangedExportDaysPeriod(sharedPreferences, key)
        }
    }

    private fun onChangedExportDaysPeriod(sharedPreferences: SharedPreferences, key: String) {
        val days = sharedPreferences.getInt(key, appSupport.exportDaysPeriod)
        val lastFollowersUpdateTime = appSupport.lastFollowersUpdateTime
        val remainingMessage: String
        remainingMessage = if (lastFollowersUpdateTime < 0) {
            resources.getString(R.string.never_run)
        } else {
            val remainingDays = (days - lastFollowersUpdateTime.daysSinceNow()).toInt()
            resources.getQuantityString(R.plurals.next_in_day, remainingDays, remainingDays)
        }
        findPreference(AppSupport.PREF_EXPORT_DAYS_PERIOD).summary =
            resources.getQuantityString(R.plurals.day_title, days, days) + " ($remainingMessage)"
    }
}