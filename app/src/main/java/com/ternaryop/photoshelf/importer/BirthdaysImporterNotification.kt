package com.ternaryop.photoshelf.importer

import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.notification.NOTIFICATION_ID_IMPORT_BIRTHDAY
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.photoshelf.util.notification.ProgressNotification
import io.reactivex.disposables.Disposable

/**
 * Created by dave on 24/03/18.
 * Import/Export functions notifying the status to user
 */

fun Importer.notifyImportBirthdaysFromWeb(blogName: String, notificationUtil: NotificationUtil): Disposable? {
    val progressNotification = ProgressNotification(notificationUtil,
        R.string.import_missing_birthdays_from_web_title,
        NOTIFICATION_ID_IMPORT_BIRTHDAY)

    return importMissingBirthdaysFromWeb(blogName)
        .doOnNext { info ->
            progressNotification
                .setProgress(info.max, info.progress, false)
                .notify(info.items.size, R.plurals.item_found)
        }
        .takeLast(1)
        .subscribe({ info -> progressNotification.notifyFinish(info.items.size, R.plurals.imported_items) }
        ) { t -> notificationUtil.notifyError(t, "Import") }
}
