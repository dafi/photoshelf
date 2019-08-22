package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.utils.date.daysSinceNow

/**
 * Created by dave on 19/02/18.
 * Export posts, birthdays and total users
 */
class BatchExporter(val appSupport: AppSupport) {
    suspend fun export() {
        val importer = Importer(appSupport)
        exportTotalUsers(importer)
    }

    private suspend fun exportTotalUsers(importer: Importer) {
        val lastUpdate = appSupport.lastFollowersUpdateTime
        if (lastUpdate < 0 || appSupport.exportDaysPeriod <= lastUpdate.daysSinceNow()) {
            try {
                val path = importer.totalUsersPath.replace(".csv", "-${appSupport.selectedBlogName!!}.csv")
                importer.syncExportTotalUsersToCSV(path, appSupport.selectedBlogName!!)
                appSupport.lastFollowersUpdateTime = System.currentTimeMillis()
            } catch (ignored: Exception) {
            }
        }
    }
}
