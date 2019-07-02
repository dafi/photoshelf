package com.ternaryop.photoshelf.util.notification

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat

/**
 * Created by dave on 03/03/18.
 * Notification builder for progress indicator
 */
class ProgressNotification(
    private val context: Context,
    @StringRes titleId: Int,
    notificationChannelId: String,
    private val notificationId: Int,
    iconId: Int) {
    var builder: NotificationCompat.Builder = NotificationCompat.Builder(context, notificationChannelId)
        .setContentText("")
        .setContentTitle(context.getString(titleId))
        .setTicker("")
        .setSmallIcon(iconId)
        .setAutoCancel(true) // remove notification when user clicks on it

    fun setProgress(max: Int, progress: Int, indeterminate: Boolean): ProgressNotification {
        builder.setProgress(max, progress, indeterminate)

        return this
    }

    fun notify(itemCount: Int, @PluralsRes pluralId: Int): ProgressNotification {
        builder.setContentText(context.resources.getQuantityString(pluralId, itemCount, itemCount))

        context.notificationManager.notify(notificationId, builder.build())

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