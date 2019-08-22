package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.notification.BIRTHDAY_CHANNEL_ID
import com.ternaryop.photoshelf.util.notification.ProgressNotification
import com.ternaryop.photoshelf.util.notification.createBirthdayChannel
import com.ternaryop.photoshelf.util.notification.notify

private const val NOTIFICATION_ID_IMPORT_BIRTHDAY = 2

/**
 * Created by dave on 24/03/18.
 * Import/Export functions notifying the status to user
 */

suspend fun Importer.notifyImportBirthdaysFromWeb() {
    // ensure channel exists
    createBirthdayChannel(context)
    val progressNotification = ProgressNotification(context,
        R.string.import_missing_birthdays_from_web_title,
        BIRTHDAY_CHANNEL_ID,
        NOTIFICATION_ID_IMPORT_BIRTHDAY,
        R.drawable.stat_notify_import_export)

    try {
        val info = importMissingBirthdaysFromWeb { info ->
            progressNotification
                .setProgress(info.max, info.progress, false)
                .notify(info.items.size, R.plurals.item_found)
        }
        progressNotification.notifyFinish(info.items.size, R.plurals.imported_items)
    } catch (t: Throwable) {
        t.printStackTrace()
        t.notify(context, "Import")
    }
}
