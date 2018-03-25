package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.notification.NOTIFICATION_ID_IMPORT_POSTS
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.photoshelf.util.notification.ProgressNotification

private const val TWEAK_NOTIFICATION_UPDATE_FREQUENCY = 1003

/**
 * Created by dave on 24/03/18.
 * Import/Export functions notifying the status to user
 */

fun Importer.notifyImportPostsFromCSV(importPath: String, notificationUtil: NotificationUtil) {
    val progressNotification = ProgressNotification(
        notificationUtil,
        R.string.import_posts_from_csv_title,
        NOTIFICATION_ID_IMPORT_POSTS)

    try {
        importPostsFromCSV(importPath)
            .doOnNext { progress ->
                // update with less frequency otherwise randomly the notification inside
                // subscribe() isn't respected (ie the progress remains visible)
                if (progress!! % TWEAK_NOTIFICATION_UPDATE_FREQUENCY == 0) {
                    progressNotification
                        .setProgress(0, 0, true)
                        .notify(progress, R.plurals.item_found)
                }
            }
            .takeLast(1)
            .subscribe({ progress -> progressNotification.notifyFinish(progress, R.plurals.imported_items) }
            ) { t -> notificationUtil.notifyError(t, "Import") }
    } catch (e: Exception) {
        notificationUtil.notifyError(e, "Import")
    }
}

fun Importer.notifyExportPostsToCSV(exportPath: String, notificationUtil: NotificationUtil) {
    try {
        val count = exportPostsToCSV(exportPath)
        val content = notificationUtil.resources.getQuantityString(R.plurals.exported_items, count, count)
        notificationUtil.notifyExport(content,
            "", "Post Export", R.drawable.stat_notify_import_export)
    } catch (e: Exception) {
        notificationUtil.notifyError(e, "Export")
    }
}
