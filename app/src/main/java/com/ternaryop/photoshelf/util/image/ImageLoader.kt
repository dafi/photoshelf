package com.ternaryop.photoshelf.util.image

import android.content.Context
import androidx.preference.PreferenceManager
import coil.Coil
import coil.ImageLoader
import com.ternaryop.photoshelf.R

const val PREF_USE_HARDWARE_IMAGES = "use_hardware_images"

object ImageLoader {
    fun setup(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        Coil.setImageLoader(
            ImageLoader.Builder(context)
                .error(com.ternaryop.photoshelf.core.R.drawable.ic_sync_problem_black_24dp)
                .allowHardware(prefs.getBoolean(PREF_USE_HARDWARE_IMAGES, true))
                .build()
        )
    }
}
