package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.notification.ProgressNotification
import com.ternaryop.photoshelf.util.notification.BIRTHDAY_CHANNEL_ID
import com.ternaryop.photoshelf.util.notification.createBirthdayChannel
import com.ternaryop.photoshelf.util.notification.notify
import io.reactivex.disposables.Disposable

private const val NOTIFICATION_ID_IMPORT_BIRTHDAY = 2

/**
 * Created by dave on 24/03/18.
 * Import/Export functions notifying the status to user
 */

fun Importer.notifyImportBirthdaysFromWeb(): Disposable? {
    // ensure channel exists
    createBirthdayChannel(context)
    val progressNotification = ProgressNotification(context,
        R.string.import_missing_birthdays_from_web_title,
        BIRTHDAY_CHANNEL_ID,
        NOTIFICATION_ID_IMPORT_BIRTHDAY,
        R.drawable.stat_notify_import_export)

    return importMissingBirthdaysFromWeb()
        .doOnNext { info ->
            progressNotification
                .setProgress(info.max, info.progress, false)
                .notify(info.items.size, R.plurals.item_found)
        }
        .takeLast(1)
        .subscribe({ info -> progressNotification.notifyFinish(info.items.size, R.plurals.imported_items) }
        ) { it.notify(context, "Import") }
}
