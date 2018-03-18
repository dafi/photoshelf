package com.ternaryop.photoshelf.fragment.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceScreen
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig
import com.ternaryop.photoshelf.service.ImportIntentService
import com.ternaryop.utils.DateTimeUtils
import java.io.File

private const val KEY_IMPORT_POSTS_FROM_CSV = "import_posts_from_csv"
private const val KEY_EXPORT_POSTS_FROM_CSV = "export_posts_csv"
private const val KEY_IMPORT_TITLE_PARSER = "import_title_parser"
private const val KEY_IMPORT_BIRTHDAYS = "import_birthdays"
private const val KEY_EXPORT_BIRTHDAYS = "export_birthdays"
private const val KEY_EXPORT_MISSING_BIRTHDAYS = "export_missing_birthdays"
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
            val csvPath = Importer.postsPath
            setupPreferenceFilePath(csvPath, KEY_IMPORT_POSTS_FROM_CSV, preferenceScreen)

            findPreference(KEY_EXPORT_POSTS_FROM_CSV).summary = csvPath

            setupPreferenceFilePath(Importer.titleParserPath, KEY_IMPORT_TITLE_PARSER, preferenceScreen)
                .title = getString(R.string.import_title_parser_title, AndroidTitleParserConfig(context!!).version)

            val birthdaysPath = Importer.birthdaysPath
            setupPreferenceFilePath(birthdaysPath, KEY_IMPORT_BIRTHDAYS, preferenceScreen)

            findPreference(KEY_EXPORT_BIRTHDAYS).summary = birthdaysPath

            findPreference(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA)

            findPreference(KEY_EXPORT_MISSING_BIRTHDAYS)

            findPreference(AppSupport.PREF_EXPORT_DAYS_PERIOD)
            onSharedPreferenceChanged(preferenceManager.sharedPreferences, AppSupport.PREF_EXPORT_DAYS_PERIOD)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            KEY_IMPORT_POSTS_FROM_CSV -> {
                ImportIntentService.startImportPostsFromCSV(context!!, Importer.postsPath)
                return true
            }
            KEY_IMPORT_TITLE_PARSER -> {
                importer.importFile(Importer.titleParserPath, Importer.TITLE_PARSER_FILE_NAME)
                return true
            }
            KEY_IMPORT_BIRTHDAYS -> {
                ImportIntentService.startImportBirthdaysFromCSV(context!!, Importer.birthdaysPath)
                return true
            }
            KEY_EXPORT_POSTS_FROM_CSV -> {
                ImportIntentService.startExportPostsCSV(context!!, Importer.getExportPath(Importer.postsPath))
                return true
            }
            KEY_EXPORT_BIRTHDAYS -> {
                ImportIntentService.startExportBirthdaysCSV(context!!, Importer.getExportPath(Importer.birthdaysPath))
                return true
            }
            KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA -> {
                ImportIntentService.startImportBirthdaysFromWeb(context!!, appSupport.selectedBlogName!!)
                return true
            }
            KEY_EXPORT_MISSING_BIRTHDAYS -> {
                ImportIntentService.startExportMissingBirthdaysCSV(context!!,
                    Importer.getExportPath(Importer.missingBirthdaysPath),
                    appSupport.selectedBlogName!!)
                return true
            }
            else -> return super.onPreferenceTreeClick(preference)
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
            val remainingDays = (days - DateTimeUtils.daysSinceTimestamp(lastFollowersUpdateTime)).toInt()
            resources.getQuantityString(R.plurals.next_in_day, remainingDays, remainingDays)
        }
        findPreference(AppSupport.PREF_EXPORT_DAYS_PERIOD).summary =
            resources.getQuantityString(R.plurals.day_title, days, days) + " ($remainingMessage)"
    }
}