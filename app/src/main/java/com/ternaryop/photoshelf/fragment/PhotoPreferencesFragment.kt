package com.ternaryop.photoshelf.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.preference.PreferenceScreen
import android.support.v7.app.ActionBar
import android.support.v7.app.AppCompatActivity
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig
import com.ternaryop.photoshelf.service.ImportIntentService
import com.ternaryop.photoshelf.util.security.PermissionUtil
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.utils.DateTimeUtils
import java.io.File

private const val TUMBLR_SERVICE_NAME = "Tumblr"
private const val DROPBOX_SERVICE_NAME = "Dropbox"

private const val KEY_TUMBLR_LOGIN = "tumblr_login"
private const val KEY_DROPBOX_LOGIN = "dropbox_login"
private const val KEY_IMPORT_POSTS_FROM_CSV = "import_posts_from_csv"
private const val KEY_EXPORT_POSTS_FROM_CSV = "export_posts_csv"
private const val KEY_IMPORT_TITLE_PARSER = "import_title_parser"
private const val KEY_IMPORT_BIRTHDAYS = "import_birthdays"
private const val KEY_EXPORT_BIRTHDAYS = "export_birthdays"
private const val KEY_EXPORT_MISSING_BIRTHDAYS = "export_missing_birthdays"
private const val KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA = "import_birthdays_from_wikipedia"
private const val KEY_CLEAR_IMAGE_CACHE = "clear_image_cache"
private const val KEY_VERSION = "version"
private const val KEY_DROPBOX_VERSION = "dropbox_version"
private const val KEY_THUMBNAIL_WIDTH = "thumbnail_width"

private const val DROPBOX_RESULT = 2
private const val REQUEST_FILE_PERMISSION = 1

class PhotoPreferencesFragment : PreferenceFragment(), OnSharedPreferenceChangeListener {

    private lateinit var preferenceTumblrLogin: Preference
    private lateinit var preferenceImportPostsFromCSV: Preference
    private lateinit var preferenceExportPostsToCSV: Preference
    private lateinit var preferenceImportTitleParser: Preference
    private lateinit var preferenceImportBirthdays: Preference
    private lateinit var preferenceExportBirthdays: Preference
    private lateinit var preferenceImportBirthdaysFromWikipedia: Preference
    private lateinit var preferenceScheduleTimeSpan: Preference
    private lateinit var preferenceClearImageCache: Preference
    private lateinit var preferenceExportMissingBirthdays: Preference
    private lateinit var preferenceDropboxLogin: Preference
    private lateinit var preferenceThumbnailWidth: ListPreference
    private lateinit var preferenceExportDaysPeriod: Preference

    private lateinit var appSupport: AppSupport
    private lateinit var dropboxManager: DropboxManager
    private val importer: Importer
        get() = Importer(activity, dropboxManager)

    private val supportActionBar: ActionBar?
        get() = (activity as AppCompatActivity).supportActionBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.settings_main)

        appSupport = AppSupport(activity)
        dropboxManager = DropboxManager.getInstance(activity)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        with(preferenceScreen) {
            preferenceTumblrLogin = findPreference(KEY_TUMBLR_LOGIN)

            if (Tumblr.isLogged(activity)) {
                preferenceTumblrLogin.title = getString(R.string.logout_title, TUMBLR_SERVICE_NAME)
            } else {
                preferenceTumblrLogin.title = getString(R.string.login_title, TUMBLR_SERVICE_NAME)
            }

            preferenceDropboxLogin = findPreference(KEY_DROPBOX_LOGIN)
            if (dropboxManager.isLinked) {
                preferenceDropboxLogin.title = getString(R.string.logout_title, DROPBOX_SERVICE_NAME)
            } else {
                preferenceDropboxLogin.title = getString(R.string.login_title, DROPBOX_SERVICE_NAME)
            }
            PermissionUtil.askPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_FILE_PERMISSION)
            val csvPath = Importer.postsPath
            preferenceImportPostsFromCSV = setupPreferenceFilePath(csvPath, KEY_IMPORT_POSTS_FROM_CSV, preferenceScreen)

            preferenceExportPostsToCSV = findPreference(KEY_EXPORT_POSTS_FROM_CSV)
            preferenceExportPostsToCSV.summary = csvPath

            preferenceImportTitleParser = setupPreferenceFilePath(Importer.titleParserPath, KEY_IMPORT_TITLE_PARSER, preferenceScreen)
            preferenceImportTitleParser.title = getString(R.string.import_title_parser_title, AndroidTitleParserConfig(activity).version)

            val birthdaysPath = Importer.birthdaysPath
            preferenceImportBirthdays = setupPreferenceFilePath(birthdaysPath, KEY_IMPORT_BIRTHDAYS, preferenceScreen)

            preferenceExportBirthdays = findPreference(KEY_EXPORT_BIRTHDAYS)
            preferenceExportBirthdays.summary = birthdaysPath

            preferenceImportBirthdaysFromWikipedia = findPreference(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA)

            preferenceScheduleTimeSpan = findPreference(AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN)
            onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(activity), AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN)

            preferenceClearImageCache = findPreference(KEY_CLEAR_IMAGE_CACHE)

            preferenceExportMissingBirthdays = findPreference(KEY_EXPORT_MISSING_BIRTHDAYS)

            preferenceThumbnailWidth = findPreference(KEY_THUMBNAIL_WIDTH) as ListPreference
            onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(activity), KEY_THUMBNAIL_WIDTH)

            preferenceExportDaysPeriod = findPreference(AppSupport.PREF_EXPORT_DAYS_PERIOD)
            onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(activity), AppSupport.PREF_EXPORT_DAYS_PERIOD)

            setupVersionInfo(preferenceScreen)
        }
    }

    private fun setupPreferenceFilePath(fullPath: String, prefKey: String, preferenceScreen: PreferenceScreen): Preference {
        val pref = preferenceScreen.findPreference(prefKey)
        pref.summary = fullPath
        pref.isEnabled = File(fullPath).exists()
        return pref
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN -> onChangedScheduleMinutesTimeSpan(sharedPreferences, key)
            KEY_THUMBNAIL_WIDTH -> onChangedThumbnailWidth(sharedPreferences, key)
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
        preferenceExportDaysPeriod.summary = (resources.getQuantityString(R.plurals.day_title, days, days)
                + " ($remainingMessage)")
    }

    private fun onChangedThumbnailWidth(sharedPreferences: SharedPreferences, key: String) {
        val value = sharedPreferences.getString(key, resources.getString(R.string.thumbnail_width_value_default))
        val index = preferenceThumbnailWidth.findIndexOfValue(value)
        if (index > -1) {
            preferenceThumbnailWidth.summary = preferenceThumbnailWidth.entries[index]
        }
    }

    private fun onChangedScheduleMinutesTimeSpan(sharedPreferences: SharedPreferences, key: String) {
        val minutes = sharedPreferences.getInt(key, 0)
        preferenceScheduleTimeSpan.summary = resources.getQuantityString(R.plurals.minute_title, minutes, minutes)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DROPBOX_RESULT) {
            preferenceDropboxLogin.title = if (dropboxManager.finishAuthentication() == null) {
                getString(R.string.login_title, DROPBOX_SERVICE_NAME)
            } else {
                getString(R.string.logout_title, DROPBOX_SERVICE_NAME)
            }
        }
    }

    @Suppress("ComplexMethod")
    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen, preference: Preference): Boolean {
        when {
            preference === preferenceTumblrLogin -> {
                if (Tumblr.isLogged(activity)) {
                    logout()
                } else {
                    Tumblr.login(activity)
                }
                return true
            }
            preference === preferenceImportPostsFromCSV -> {
                ImportIntentService.startImportPostsFromCSV(activity, Importer.postsPath)
                return true
            }
            else -> when {
                preference === preferenceImportTitleParser -> {
                    importer.importFile(Importer.titleParserPath, Importer.TITLE_PARSER_FILE_NAME)
                    return true
                }
                preference === preferenceImportBirthdays -> {
                    ImportIntentService.startImportBirthdaysFromCSV(activity, Importer.birthdaysPath)
                    return true
                }
                preference === preferenceExportPostsToCSV -> {
                    ImportIntentService.startExportPostsCSV(activity, Importer.getExportPath(Importer.postsPath))
                    return true
                }
                preference === preferenceExportBirthdays -> {
                    ImportIntentService.startExportBirthdaysCSV(activity, Importer.getExportPath(Importer.birthdaysPath))
                    return true
                }
                preference === preferenceImportBirthdaysFromWikipedia -> {
                    ImportIntentService.startImportBirthdaysFromWeb(activity, appSupport.selectedBlogName!!)
                    return true
                }
                preference === preferenceClearImageCache -> {
                    clearImageCache()
                    return true
                }
                preference === preferenceExportMissingBirthdays -> {
                    ImportIntentService.startExportMissingBirthdaysCSV(activity,
                            Importer.getExportPath(Importer.missingBirthdaysPath),
                            appSupport.selectedBlogName!!)
                    return true
                }
                preference === preferenceDropboxLogin -> {
                    if (dropboxManager.isLinked) {
                        dropboxManager.unlink()
                        preferenceDropboxLogin.title = getString(R.string.login_title, DROPBOX_SERVICE_NAME)
                    } else {
                        DropboxManager.getInstance(activity).startOAuth2AuthenticationForResult(this, DROPBOX_RESULT)
                    }
                    return true
                }
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference)
    }

    private fun logout() {
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> Tumblr.logout(activity.applicationContext)
            }
        }

        AlertDialog.Builder(activity)
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun clearImageCache() {
        // copied from https://github.com/UweTrottmann/SeriesGuide/
        // try to open app info where user can clear app cache folders
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + activity.packageName)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // open all apps view
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    private fun setupVersionInfo(preferenceScreen: PreferenceScreen) {
        var preferenceVersion = preferenceScreen.findPreference(KEY_VERSION)
        preferenceVersion.title = getString(R.string.version_title, getString(R.string.app_name))
        preferenceVersion.summary = try {
            val versionName = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName
            val versionCode = activity.packageManager.getPackageInfo(activity.packageName, 0).versionCode
            "$versionName build $versionCode"
        } catch (e: Exception) {
            "N/A"
        }

        // dropbox
        preferenceVersion = preferenceScreen.findPreference(KEY_DROPBOX_VERSION)
        preferenceVersion.title = getString(R.string.version_title, "Dropbox")
        preferenceVersion.summary = DropboxManager.Version
    }
}
