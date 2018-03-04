package com.ternaryop.photoshelf.util.notification

import android.support.annotation.PluralsRes
import android.support.annotation.StringRes
import android.support.v4.app.NotificationCompat
import com.ternaryop.photoshelf.R

/**
 * Created by dave on 03/03/18.
 * Notification builder for progress indicator
 */
class ProgressNotification(private val notificationUtil: NotificationUtil, @StringRes titleId: Int, private val notificationId: Int) {
    var builder: NotificationCompat.Builder = notificationUtil.createNotification(
        "",
        "",
        null,
        R.drawable.stat_notify_import_export)
        .setContentTitle(notificationUtil.getString(titleId))

    fun setProgress(max: Int, progress: Int, indeterminate: Boolean): ProgressNotification {
        builder.setProgress(max, progress, indeterminate)

        return this
    }

    fun notify(itemCount: Int, @PluralsRes pluralId: Int): ProgressNotification {
        builder.setContentText(notificationUtil.resources.getQuantityString(pluralId, itemCount, itemCount))
        notificationUtil.notificationManager.notify(notificationId, builder.build())
        return this
    }

    /**
     * reset the progress counters and indeterminate state the call notify
     */
    fun notifyFinish(itemCount: Int, @PluralsRes pluralId: Int): ProgressNotification {
        return setProgress(0, 0, false)
            .notify(itemCount, pluralId)
    }
}