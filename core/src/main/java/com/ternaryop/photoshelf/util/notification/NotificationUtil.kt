package com.ternaryop.photoshelf.util.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import androidx.core.app.NotificationCompat

inline val Context.notificationManager: NotificationManager
    get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

fun NotificationCompat.Builder.setupMultiLineNotification(
    bigTitle: String,
    lines: List<String>,
    deleteIntent: PendingIntent? = null
): NotificationCompat.Builder {
    val inboxStyle = NotificationCompat.InboxStyle()
        .setBigContentTitle(bigTitle)
    for (line in lines) {
        inboxStyle.addLine(line)
    }
    setStyle(inboxStyle)
    setDeleteIntent(deleteIntent)
    return this
}
