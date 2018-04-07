package com.ternaryop.photoshelf.importer

import android.os.Environment
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.log.Log
import com.ternaryop.utils.date.daysSinceNow
import java.io.File

private const val ERROR_FILE_NAME = "export_errors.txt"

/**
 * Created by dave on 19/02/18.
 * Export posts, birthdays and total users
 */
class BatchExporter(val appSupport: AppSupport) {
    private val logPath: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ERROR_FILE_NAME)

    fun export() {
        val importer = Importer(appSupport)
        exportPosts(importer)
        exportBirthdays(importer)
        exportTotalUsers(importer)
    }

    private fun exportTotalUsers(importer: Importer) {
        val lastUpdate = appSupport.lastFollowersUpdateTime
        if (lastUpdate < 0 || appSupport.exportDaysPeriod <= lastUpdate.daysSinceNow()) {
            try {
                importer.syncExportTotalUsersToCSV(Importer.totalUsersPath, appSupport.selectedBlogName!!)
                appSupport.lastFollowersUpdateTime = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.error(e, logPath, "Export total users")
            }
        }
    }

    private fun exportBirthdays(importer: Importer) {
        try {
            importer.exportBirthdaysToCSV(Importer.birthdaysPath)
        } catch (e: Exception) {
            Log.error(e, logPath, "Export birthdays")
        }
    }

    private fun exportPosts(importer: Importer) {
        try {
            importer.exportPostsToCSV(Importer.postsPath)
        } catch (e: Exception) {
            Log.error(e, logPath, "Export posts")
        }
    }
}
