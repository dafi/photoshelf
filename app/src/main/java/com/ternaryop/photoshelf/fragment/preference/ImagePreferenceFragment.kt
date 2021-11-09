package com.ternaryop.photoshelf.fragment.preference

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.ternaryop.photoshelf.R
import com.ternaryop.preference.AppPreferenceFragment

private const val KEY_THUMBNAIL_WIDTH = "thumbnail_width"
private const val KEY_CLEAR_IMAGE_CACHE = "clear_image_cache"
private const val KEY_USE_HARDWARE_IMAGES = "use_hardware_images"

/**
 * Created by dave on 07/Jul/20.
 * Hold Image Preferences
 */
class ImagePreferenceFragment : AppPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)

        onSharedPreferenceChanged(preferenceManager.sharedPreferences, KEY_THUMBNAIL_WIDTH)
        onSharedPreferenceChanged(preferenceManager.sharedPreferences, KEY_USE_HARDWARE_IMAGES)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            KEY_CLEAR_IMAGE_CACHE -> {
                clearImageCache()
                return true
            }
            KEY_USE_HARDWARE_IMAGES -> {
                return true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences ?: return
        when (key) {
            KEY_THUMBNAIL_WIDTH -> onChangedThumbnailWidth(sharedPreferences, key)
            KEY_USE_HARDWARE_IMAGES -> onChangedUseHardwareImages(sharedPreferences, key)
        }
    }

    private fun clearImageCache() {
        // copied from https://github.com/UweTrottmann/SeriesGuide/
        // try to open app info where user can clear app cache folders
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:" + requireContext().packageName)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // open all apps view
            startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    private fun onChangedThumbnailWidth(sharedPreferences: SharedPreferences, key: String) {
        val value = sharedPreferences.getString(key,
            resources.getInteger(R.integer.thumbnail_width_value_default).toString())
        findPreference<ListPreference>(KEY_THUMBNAIL_WIDTH)?.apply {
            val index = findIndexOfValue(value)
            if (index > -1) {
                summary = entries[index]
            }
        }
    }

    private fun onChangedUseHardwareImages(sharedPreferences: SharedPreferences, key: String) {
        val findPreference = findPreference<SwitchPreferenceCompat>(KEY_USE_HARDWARE_IMAGES)
        findPreference?.isChecked =
            sharedPreferences.getBoolean(key, true)
    }
}
