package com.ternaryop.photoshelf.home.fragment

import android.content.Context
import androidx.core.content.FileProvider
import com.ternaryop.photoshelf.home.R
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.intent.ShareChooserParams
import com.ternaryop.utils.intent.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal object CrashLogViewer {
    suspend fun startView(context: Context) = withContext(Dispatchers.IO) {
        val authority = "${context.applicationInfo.packageName}.fileprovider"
        val shareChooserParams = ShareChooserParams(
            FileProvider.getUriForFile(context, authority, file(context)),
            context.resources.getString(R.string.crash_log_title),
            context.resources.getString(R.string.crash_log_subject),
            "text/plain"
        )
        try {
            ShareUtils.showShareChooser(context, shareChooserParams)
        } catch (e: Exception) {
            e.showErrorDialog(context)
        }
    }

    fun file(context: Context) =
        File(File(context.getExternalFilesDir(null), "crash"), "crash_report.txt")
}