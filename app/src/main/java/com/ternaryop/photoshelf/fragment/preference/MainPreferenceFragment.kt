package com.ternaryop.photoshelf.fragment.preference

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.ternaryop.compat.content.getPackageInfoCompat
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.core.prefs.PREF_KEY_SHOW_PICKER_SHARE_MENU
import com.ternaryop.photoshelf.core.prefs.PREF_PHOTOSHELF_APIKEY
import com.ternaryop.photoshelf.core.prefs.clearBlogList
import com.ternaryop.photoshelf.core.prefs.showPickerShareMenu
import com.ternaryop.photoshelf.util.app.showImagePickerOnShareMenu
import com.ternaryop.preference.AppPreferenceFragment
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
private const val KEY_VERSION = "version"
private const val KEY_DROPBOX_VERSION = "dropbox_version"
private const val KEY_SCHEDULE_MINUTES_TIME_SPAN = "schedule_minutes_time_span"
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
        PermissionUtil.askPermission(
            requireActivity(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            REQUEST_FILE_PERMISSION,
            AlertDialog.Builder(activity).setMessage(R.string.import_permission_rationale)
        )

        setupMinutesTimeSpanSummary()

        setupVersionInfo(preferenceScreen)
    }

    private fun setupMinutesTimeSpanSummary() {
        findPreference<EditTextPreference>(KEY_SCHEDULE_MINUTES_TIME_SPAN)
            ?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
            val minutes = preference.text?.toInt() ?: 0
            resources.getQuantityString(R.plurals.minute_title, minutes, minutes)
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            PREF_PHOTOSHELF_APIKEY -> ApiManager.updateToken(
                sharedPreferences.getString(key, null) ?: ""
            )
            PREF_KEY_SHOW_PICKER_SHARE_MENU -> requireContext().packageManager.showImagePickerOnShareMenu(
                requireContext().packageName,
                sharedPreferences.showPickerShareMenu
            )
        }
    }

    override fun onResume() {
        super.onResume()
        findPreference<Preference>(KEY_DROPBOX_LOGIN)?.title =
            if (dropboxManager.finishAuthentication() == null) {
                getString(R.string.login_title, DROPBOX_SERVICE_NAME)
            } else {
                getString(R.string.logout_title, DROPBOX_SERVICE_NAME)
            }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
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
            KEY_DROPBOX_LOGIN -> {
                if (dropboxManager.isLinked) {
                    dropboxManager.unlink()
                    preference.title = getString(R.string.login_title, DROPBOX_SERVICE_NAME)
                } else {
                    DropboxManager.getInstance(requireContext())
                        .startOAuth2Authentication(this)
                }
                return true
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
    }

    private fun logout() {
        val dialogClickListener = DialogInterface.OnClickListener { _, _ ->
            TumblrManager.logout(requireContext())
            preferenceScreen.sharedPreferences?.clearBlogList()
            toggleTumblrLoginTitle()
        }

        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.are_you_sure))
            .setPositiveButton(android.R.string.ok, dialogClickListener)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun setupVersionInfo(preferenceScreen: PreferenceScreen) {
        preferenceScreen.findPreference<Preference>(KEY_VERSION)?.apply {
            title = getString(R.string.version_title, getString(R.string.app_name))
            summary = try {
                val packageInfo = requireContext().packageManager.getPackageInfoCompat(requireContext().packageName, 0)
                val versionName = packageInfo.versionName
                val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
                "$versionName build $versionCode"
            } catch (ignored: Exception) {
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
