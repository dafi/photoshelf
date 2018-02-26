package com.ternaryop.photoshelf.importer

import android.os.Environment
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.photoshelf.util.log.Log
import com.ternaryop.utils.DateTimeUtils
import java.io.File

/**
 * Created by dave on 19/02/18.
 * Export posts, birthdays and total users
 */
class BatchExporter(val appSupport: AppSupport) {
    private val logPath: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "export_errors.txt")

    fun export() {
        val importer = Importer(appSupport, DropboxManager.getInstance(appSupport))
        exportPosts(importer)
        exportBirthdays(importer)
        exportTotalUsers(importer)
    }

    private fun exportTotalUsers(importer: Importer) {
        val lastUpdate = appSupport.lastFollowersUpdateTime
        if (lastUpdate < 0 || appSupport.exportDaysPeriod <= DateTimeUtils.daysSinceTimestamp(lastUpdate)) {
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
