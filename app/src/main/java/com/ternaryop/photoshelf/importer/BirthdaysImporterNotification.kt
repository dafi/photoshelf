package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.notification.NOTIFICATION_ID_IMPORT_BIRTHDAY
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.photoshelf.util.notification.ProgressNotification

/**
 * Created by dave on 24/03/18.
 * Import/Export functions notifying the status to user
 */

fun Importer.notifyImportBirthdays(importPath: String, notificationUtil: NotificationUtil) {
    try {
        importBirthdays(importPath)
            .subscribe({ total ->
                val content = notificationUtil.resources.getQuantityString(R.plurals.imported_items, total!!, total)
                notificationUtil.notifyExport(content,
                    "", "Birthdays Import", R.drawable.stat_notify_import_export)
            }
            ) { t -> notificationUtil.notifyError(t, "Import") }
    } catch (e: Exception) {
        notificationUtil.notifyError(e, "Import")
    }
}

fun Importer.notifyExportBirthdaysToCSV(exportPath: String, notificationUtil: NotificationUtil) {
    try {
        val count = exportBirthdaysToCSV(exportPath)
        val content = notificationUtil.resources.getQuantityString(R.plurals.exported_items, count, count)
        notificationUtil.notifyExport(content,
            "", "Birthdays Export", R.drawable.stat_notify_import_export)
    } catch (e: Exception) {
        notificationUtil.notifyError(e, "Export")
    }
}

fun Importer.notifyExportMissingBirthdaysToCSV(exportPath: String,
    blogName: String, notificationUtil: NotificationUtil) {
    try {
        val count = exportMissingBirthdaysToCSV(exportPath, blogName)
        val content = notificationUtil.resources.getQuantityString(R.plurals.exported_items, count, count)
        notificationUtil.notifyExport(content,
            "", "Missing Birthdays Export", R.drawable.stat_notify_import_export)
    } catch (e: Exception) {
        notificationUtil.notifyError(e, "Export")
    }
}

fun Importer.notifyImportBirthdaysFromWeb(blogName: String, notificationUtil: NotificationUtil) {
    val progressNotification = ProgressNotification(notificationUtil,
        R.string.import_missing_birthdays_from_web_title,
        NOTIFICATION_ID_IMPORT_BIRTHDAY)

    importMissingBirthdaysFromWeb(blogName)
        .doOnNext { info ->
            progressNotification
                .setProgress(info.max, info.progress, false)
                .notify(info.items.size, R.plurals.item_found)
        }
        .takeLast(1)
        .subscribe({ info -> progressNotification.notifyFinish(info.items.size, R.plurals.imported_items) }
        ) { t -> notificationUtil.notifyError(t, "Import") }
}
