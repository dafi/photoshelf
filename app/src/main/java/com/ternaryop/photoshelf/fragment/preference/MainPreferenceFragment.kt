package com.ternaryop.photoshelf.fragment.preference

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.photoshelf.util.security.PermissionUtil
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.utils.dialog.showErrorDialog

private const val TUMBLR_SERVICE_NAME = "Tumblr"
private const val DROPBOX_SERVICE_NAME = "Dropbox"

private const val KEY_TUMBLR_LOGIN = "tumblr_login"
private const val KEY_DROPBOX_LOGIN = "dropbox_login"
private const val KEY_CLEAR_IMAGE_CACHE = "clear_image_cache"
private const val KEY_VERSION = "version"
private const val KEY_DROPBOX_VERSION = "dropbox_version"
private const val KEY_THUMBNAIL_WIDTH = "thumbnail_width"

private const val DROPBOX_RESULT = 2
private const val REQUEST_FILE_PERMISSION = 1

class MainPreferenceFragment : AppPreferenceFragment() {

    private lateinit var appSupport: AppSupport
    private lateinit var dropboxManager: DropboxManager

    private val supportActionBar: ActionBar?
        get() = (context!! as AppCompatActivity).supportActionBar

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        appSupport = AppSupport(context!!)
        dropboxManager = DropboxManager.getInstance(context!!)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toggleTumblrLoginTitle()
        toggleDropboxLoginTitle()
        PermissionUtil.askPermission(activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_FILE_PERMISSION)

        findPreference(AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN)

        onSharedPreferenceChanged(preferenceManager.sharedPreferences, KEY_THUMBNAIL_WIDTH)

        onSharedPreferenceChanged(preferenceManager.sharedPreferences, AppSupport.PREF_EXPORT_DAYS_PERIOD)

        setupVersionInfo(preferenceScreen)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN -> onChangedScheduleMinutesTimeSpan(sharedPreferences, key)
            KEY_THUMBNAIL_WIDTH -> onChangedThumbnailWidth(sharedPreferences, key)
        }
    }

    private fun onChangedThumbnailWidth(sharedPreferences: SharedPreferences, key: String) {
        val value = sharedPreferences.getString(key, resources.getString(R.string.thumbnail_width_value_default))
        (findPreference(KEY_THUMBNAIL_WIDTH) as ListPreference).apply {
            val index = findIndexOfValue(value)
            if (index > -1) {
                summary = entries[index]
            }
        }
    }

    private fun onChangedScheduleMinutesTimeSpan(sharedPreferences: SharedPreferences, key: String) {
        val minutes = sharedPreferences.getInt(key, 0)
        findPreference(AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN)
            .summary = resources.getQuantityString(R.plurals.minute_title, minutes, minutes)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DROPBOX_RESULT) {
            findPreference(KEY_DROPBOX_LOGIN).title = if (dropboxManager.finishAuthentication() == null) {
                getString(R.string.login_title, DROPBOX_SERVICE_NAME)
            } else {
                getString(R.string.logout_title, DROPBOX_SERVICE_NAME)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            KEY_TUMBLR_LOGIN -> {
                if (TumblrManager.isLogged(context!!)) {
                    logout()
                } else {
                    TumblrManager
                        .login(context!!)
                        .subscribe({}, { it.showErrorDialog(context!!) })
                }
                return true
            }
            KEY_CLEAR_IMAGE_CACHE -> {
                clearImageCache()
                return true
            }
            KEY_DROPBOX_LOGIN -> {
                if (dropboxManager.isLinked) {
                    dropboxManager.unlink()
                    preference.title = getString(R.string.login_title, DROPBOX_SERVICE_NAME)
                } else {
                    DropboxManager.getInstance(context!!)
                        .startOAuth2AuthenticationForResult(this, DROPBOX_RESULT)
                }
                return true
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
    }

    private fun logout() {
        val dialogClickListener = DialogInterface.OnClickListener { _, _ ->
            TumblrManager.logout(context!!)
            appSupport.clearBlogList()
            toggleTumblrLoginTitle()
        }

        AlertDialog.Builder(context!!)
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun clearImageCache() {
        // copied from https://github.com/UweTrottmann/SeriesGuide/
        // try to open app info where user can clear app cache folders
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + context!!.packageName)
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
            val versionName = context!!.packageManager.getPackageInfo(context!!.packageName, 0).versionName
            val versionCode = context!!.packageManager.getPackageInfo(context!!.packageName, 0).versionCode
            "$versionName build $versionCode"
        } catch (e: Exception) {
            "N/A"
        }

        // dropbox
        preferenceVersion = preferenceScreen.findPreference(KEY_DROPBOX_VERSION)
        preferenceVersion.title = getString(R.string.version_title, "Dropbox")
        preferenceVersion.summary = DropboxManager.Version
    }

    private fun toggleTumblrLoginTitle() {
        findPreference(KEY_TUMBLR_LOGIN).apply {
            title = if (TumblrManager.isLogged(context!!)) {
                getString(R.string.logout_title, TUMBLR_SERVICE_NAME)
            } else {
                getString(R.string.login_title, TUMBLR_SERVICE_NAME)
            }
        }
    }

    private fun toggleDropboxLoginTitle() {
        findPreference(KEY_DROPBOX_LOGIN).apply {
            title = if (dropboxManager.isLinked) {
                getString(R.string.logout_title, DROPBOX_SERVICE_NAME)
            } else {
                getString(R.string.login_title, DROPBOX_SERVICE_NAME)
            }
        }
    }
}
