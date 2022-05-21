package com.ternaryop.photoshelf.fragment.preference

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.preference.Preference
import com.ternaryop.photoshelf.R
import com.ternaryop.preference.AppPreferenceFragment

private const val KEY_CLEAR_IMAGE_CACHE = "clear_image_cache"
private const val KEY_USE_HARDWARE_IMAGES = "use_hardware_images"

/**
 * Created by dave on 07/Jul/20.
 * Hold Image Preferences
 */
class ImagePreferenceFragment : AppPreferenceFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)
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
}
