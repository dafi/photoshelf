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
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.core.prefs.PREF_EXPORT_DAYS_PERIOD
import com.ternaryop.photoshelf.core.prefs.PREF_PHOTOSHELF_APIKEY
import com.ternaryop.photoshelf.core.prefs.clearBlogList
import com.ternaryop.photoshelf.tumblr.ui.core.prefs.PREF_THUMBNAIL_WIDTH
import com.ternaryop.photoshelf.tumblr.ui.draft.prefs.PREF_SCHEDULE_MINUTES_TIME_SPAN
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.dropbox.DropboxManager
import com.ternaryop.utils.security.PermissionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val TUMBLR_SERVICE_NAME = "Tumblr"
private const val DROPBOX_SERVICE_NAME = "Dropbox"

private const val KEY_TUMBLR_LOGIN = "tumblr_login"
private const val KEY_DROPBOX_LOGIN = "dropbox_login"
private const val KEY_CLEAR_IMAGE_CACHE = "clear_image_cache"
private const val KEY_VERSION = "version"
private const val KEY_DROPBOX_VERSION = "dropbox_version"

private const val DROPBOX_RESULT = 2
private const val REQUEST_FILE_PERMISSION = 1

class MainPreferenceFragment : AppPreferenceFragment(), CoroutineScope {

    private lateinit var dropboxManager: DropboxManager
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val supportActionBar: ActionBar?
        get() = (requireContext() as AppCompatActivity).supportActionBar

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        dropboxManager = DropboxManager.getInstance(requireContext())
        job = Job()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toggleTumblrLoginTitle()
        toggleDropboxLoginTitle()
        PermissionUtil.askPermission(requireActivity(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            REQUEST_FILE_PERMISSION,
            AlertDialog.Builder(activity).setMessage(R.string.import_permission_rationale))

        findPreference<Preference>(PREF_SCHEDULE_MINUTES_TIME_SPAN)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, PREF_SCHEDULE_MINUTES_TIME_SPAN)

        onSharedPreferenceChanged(preferenceManager.sharedPreferences, PREF_THUMBNAIL_WIDTH)

        onSharedPreferenceChanged(preferenceManager.sharedPreferences, PREF_EXPORT_DAYS_PERIOD)

        setupVersionInfo(preferenceScreen)
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PREF_SCHEDULE_MINUTES_TIME_SPAN -> onChangedScheduleMinutesTimeSpan(sharedPreferences, key)
            PREF_THUMBNAIL_WIDTH -> onChangedThumbnailWidth(sharedPreferences, key)
            PREF_PHOTOSHELF_APIKEY -> ApiManager.updateToken(sharedPreferences.getString(key, null) ?: "")
        }
    }

    private fun onChangedThumbnailWidth(sharedPreferences: SharedPreferences, key: String) {
        val value = sharedPreferences.getString(key,
            resources.getInteger(R.integer.thumbnail_width_value_default).toString())
        findPreference<ListPreference>(PREF_THUMBNAIL_WIDTH)?.apply {
            val index = findIndexOfValue(value)
            if (index > -1) {
                summary = entries[index]
            }
        }
    }

    private fun onChangedScheduleMinutesTimeSpan(sharedPreferences: SharedPreferences, key: String) {
        val minutes = sharedPreferences.getInt(key, 0)
        findPreference<Preference>(PREF_SCHEDULE_MINUTES_TIME_SPAN)
            ?.summary = resources.getQuantityString(R.plurals.minute_title, minutes, minutes)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == DROPBOX_RESULT) {
            findPreference<Preference>(KEY_DROPBOX_LOGIN)?.title = if (dropboxManager.finishAuthentication() == null) {
                getString(R.string.login_title, DROPBOX_SERVICE_NAME)
            } else {
                getString(R.string.logout_title, DROPBOX_SERVICE_NAME)
            }
        }
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            KEY_TUMBLR_LOGIN -> {
                if (TumblrManager.isLogged(requireContext())) {
                    logout()
                } else {
                    launch(Dispatchers.IO) {
                        try {
                            TumblrManager.login(requireContext())
                        } catch (t: Throwable) {
                            t.showErrorDialog(requireContext())
                        }
                    }
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
                    DropboxManager.getInstance(requireContext())
                        .startOAuth2AuthenticationForResult(this, DROPBOX_RESULT)
                }
                return true
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
    }

    private fun logout() {
        val dialogClickListener = DialogInterface.OnClickListener { _, _ ->
            TumblrManager.logout(requireContext())
            preferenceScreen.sharedPreferences.clearBlogList()
            toggleTumblrLoginTitle()
        }

        AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.are_you_sure))
                .setPositiveButton(android.R.string.yes, dialogClickListener)
                .setNegativeButton(android.R.string.no, null)
                .show()
    }

    private fun clearImageCache() {
        // copied from https://github.com/UweTrottmann/SeriesGuide/
        // try to open app info where user can clear app cache folders
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + requireContext().packageName)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // open all apps view
            startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    private fun setupVersionInfo(preferenceScreen: PreferenceScreen) {
        preferenceScreen.findPreference<Preference>(KEY_VERSION)?.apply {
            title = getString(R.string.version_title, getString(R.string.app_name))
            summary = try {
                val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                val versionName = packageInfo.versionName
                val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
                "$versionName build $versionCode"
            } catch (e: Exception) {
                "N/A"
            }
        }

        // dropbox
        preferenceScreen.findPreference<Preference>(KEY_DROPBOX_VERSION)?.apply {
            title = getString(R.string.version_title, "Dropbox")
            summary = DropboxManager.Version
        }
    }

    private fun toggleTumblrLoginTitle() {
        findPreference<Preference>(KEY_TUMBLR_LOGIN)?.apply {
            title = if (TumblrManager.isLogged(requireContext())) {
                getString(R.string.logout_title, TUMBLR_SERVICE_NAME)
            } else {
                getString(R.string.login_title, TUMBLR_SERVICE_NAME)
            }
        }
    }

    private fun toggleDropboxLoginTitle() {
        findPreference<Preference>(KEY_DROPBOX_LOGIN)?.apply {
            title = if (dropboxManager.isLinked) {
                getString(R.string.logout_title, DROPBOX_SERVICE_NAME)
            } else {
                getString(R.string.login_title, DROPBOX_SERVICE_NAME)
            }
        }
    }
}
