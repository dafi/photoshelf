package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.widget.Toast
import androidx.annotation.StringRes
import com.ternaryop.photoshelf.EXTRA_URI
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.utils.bitmap.readBitmap
import com.ternaryop.utils.bitmap.scale
import java.net.URL

/**
 * Created by dave on 01/03/14.
 * Intent used to change wallpaper
 */
class WallpaperIntentService : IntentService("wallpaperIntent") {

    private lateinit var notificationUtil: NotificationUtil
    private val handler = Handler()

    override fun onCreate() {
        super.onCreate()
        notificationUtil = NotificationUtil(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_CHANGE_WALLPAPER -> changeWallpaper(intent)
        }
    }

    private fun changeWallpaper(intent: Intent) {
        try {
            val imageUrl = intent.getParcelableExtra<Uri>(EXTRA_URI) ?: return
            val metrics = resources.displayMetrics
            val bitmap = URL(imageUrl.toString())
                .readBitmap().scale(metrics.widthPixels, metrics.heightPixels, true)
            WallpaperManager.getInstance(this).setBitmap(bitmap)
            showToast(R.string.wallpaper_changed_title)
        } catch (e: Exception) {
            notificationUtil.notifyError(e, "")
        }
    }

    private fun showToast(@StringRes res: Int) {
        handler.post { Toast.makeText(applicationContext, res, Toast.LENGTH_LONG).show() }
    }

    companion object {
        private const val ACTION_CHANGE_WALLPAPER = "changeWallpaper"

        fun startChangeWallpaperIntent(context: Context,
                                       imageUri: Uri) {
            val intent = Intent(context, WallpaperIntentService::class.java)
                .setAction(ACTION_CHANGE_WALLPAPER)
                .putExtra(EXTRA_URI, imageUri)

            context.startService(intent)
        }
    }
}
