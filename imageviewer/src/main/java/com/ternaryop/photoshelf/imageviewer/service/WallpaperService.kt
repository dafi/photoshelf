package com.ternaryop.photoshelf.imageviewer.service

import android.app.WallpaperManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ternaryop.photoshelf.imageviewer.R
import com.ternaryop.photoshelf.util.notification.notify
import com.ternaryop.utils.bitmap.readBitmap
import com.ternaryop.utils.bitmap.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val PARAM_IMAGE_URL = "imageUrl"

/**
 * Created by dave on 01/03/14.
 * Intent used to change wallpaper
 */
class WallpaperService(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        changeWallpaper()
        return Result.success()
    }

    private suspend fun changeWallpaper() {
        with(applicationContext) {
            try {
                val imageUrl = inputData.getString(PARAM_IMAGE_URL) ?: return
                val metrics = resources.displayMetrics
                withContext(Dispatchers.IO) {
                    val bitmap = URL(imageUrl)
                        .readBitmap().scale(metrics.widthPixels, metrics.heightPixels, true)
                    WallpaperManager.getInstance(applicationContext).setBitmap(bitmap)
                }
                showToast(R.string.wallpaper_changed_title)
            } catch (e: Exception) {
                e.notify(this, "")
            }
        }
    }

    private suspend fun showToast(@StringRes res: Int) {
        withContext(Dispatchers.Main) {
            Toast.makeText(applicationContext, res, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        fun startChange(
            context: Context,
            imageUrl: String
        ) {
            val data = workDataOf(
                PARAM_IMAGE_URL to imageUrl
            )
            val changeWorkerResult = OneTimeWorkRequestBuilder<WallpaperService>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(changeWorkerResult)
        }
    }
}
