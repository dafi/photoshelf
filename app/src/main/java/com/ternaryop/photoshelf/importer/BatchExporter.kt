package com.ternaryop.photoshelf.importer

import android.content.Context
import androidx.preference.PreferenceManager
import com.ternaryop.photoshelf.core.prefs.exportDaysPeriod
import com.ternaryop.photoshelf.core.prefs.lastFollowersUpdateTime
import com.ternaryop.photoshelf.core.prefs.selectedBlogName
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.utils.date.daysSinceNow

/**
 * Created by dave on 19/02/18.
 * Export posts, birthdays and total users
 */
class BatchExporter(val context: Context) {
    suspend fun export() {
        val importer = Importer(context)
        exportTotalUsers(importer)
    }

    private suspend fun exportTotalUsers(importer: Importer) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lastUpdate = prefs.lastFollowersUpdateTime
        if (lastUpdate < 0 || prefs.exportDaysPeriod(context) <= lastUpdate.daysSinceNow()) {
            val blogName = checkNotNull(prefs.selectedBlogName)
            try {
                val path = importer.totalUsersPath.replace(".csv", "-$blogName.csv")
                importer.syncExportTotalUsersToCSV(path, blogName)
                prefs.lastFollowersUpdateTime = System.currentTimeMillis()
            } catch (ignored: Exception) {
            }
        }
    }
}
