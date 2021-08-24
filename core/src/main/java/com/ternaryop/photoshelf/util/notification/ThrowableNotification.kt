package com.ternaryop.photoshelf.util.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import com.ternaryop.photoshelf.core.R
import com.ternaryop.utils.dialog.getExceptionMessageChain

const val THROWABLE_CHANNEL_ID = "errorId"
const val THROWABLE_CHANNEL_NAME = "Error"
const val EXTRA_NOTIFICATION_TAG = "com.ternaryop.photoshelf.util.extra.NOTIFICATION_TAG"

private const val NOTIFICATION_TAG = "com.ternaryop.notification.error"

fun Throwable.notify(context: Context, title: String, actionTitle: String, intent: Intent, offsetId: Int) {
    intent.putExtra(EXTRA_NOTIFICATION_TAG, NOTIFICATION_TAG)
    // use offsetId to preserve extras for different notifications
    val pendingIntent = PendingIntent.getBroadcast(context, offsetId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)

    val builder = NotificationCompat.Builder(context, THROWABLE_CHANNEL_ID)
        .setSmallIcon(R.drawable.stat_notify_error)
        .setContentTitle(title)
        .setContentText(localizedMessage)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .addAction(R.drawable.stat_notify_error, actionTitle, pendingIntent)

    createErrorNotificationChannel(context).notify(NOTIFICATION_TAG, offsetId, builder.build())
}

fun Throwable.notify(context: Context, title: String, ticker: String? = null, offsetId: Int = 0) {
    val builder = NotificationCompat.Builder(context, THROWABLE_CHANNEL_ID)
        .setSmallIcon(R.drawable.stat_notify_error)
        .setContentText(title)
        .setTicker(ticker)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setAutoCancel(true) // remove notification when user clicks on it
        .setupMultiLineNotification("Error", getExceptionMessageChain())
    // add offsetId to ensure every notification is shown

    createErrorNotificationChannel(context).notify(NOTIFICATION_TAG, offsetId, builder.build())
}

@Suppress("MagicNumber")
fun createErrorNotificationChannel(context: Context): NotificationManager {
    val channel = NotificationChannel(THROWABLE_CHANNEL_ID, THROWABLE_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
    channel.enableLights(true)
    channel.lightColor = Color.RED
    channel.enableVibration(true)
    channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
    return context.notificationManager.also { it.createNotificationChannel(channel) }
}
