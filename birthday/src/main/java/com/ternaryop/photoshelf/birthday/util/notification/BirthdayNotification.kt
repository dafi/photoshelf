package com.ternaryop.photoshelf.birthday.util.notification

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.util.notification.notificationManager
import com.ternaryop.utils.date.year
import com.ternaryop.utils.date.yearsBetweenDates

const val BIRTHDAY_CHANNEL_ID = "birthdayId"
const val BIRTHDAY_CHANNEL_NAME = "Birthdays"

const val BIRTHDAY_NOTIFICATION_ID = 1

private const val BIRTHDAY_TODAY_TAG = "com.ternaryop.photoshelf.birthday.today"

fun List<Birthday>.notifyTodayBirthdays(context: Context, currYear: Int, activityClass: Class<out Activity>) {
    if (isEmpty()) {
        return
    }

    val builder = NotificationCompat.Builder(context, BIRTHDAY_CHANNEL_ID)
        .setContentIntent(createPendingIntent(context, activityClass))
        .setSmallIcon(R.drawable.stat_notify_bday)
        .setAutoCancel(true) // remove notification when user clicks on it

    if (size == 1) {
        val birthday = this[0]
        builder.setContentTitle(context.resources.getQuantityString(R.plurals.birthday_title, size))
        builder.setContentText(
            context.getString(R.string.birthday_years_old, birthday.name, birthday.birthdate.yearsBetweenDates()))
    } else {
        builder.setStyle(buildBirthdayStyle(context, currYear))
        // The text is shown when there isn't enough space for inboxStyle
        builder.setContentTitle(context.resources.getQuantityString(R.plurals.birthday_title, size, size))
        builder.setContentText(TextUtils.join(", ", map { it.name }))
    }

    createBirthdayChannel(context).notify(BIRTHDAY_TODAY_TAG, BIRTHDAY_NOTIFICATION_ID, builder.build())
}

private fun List<Birthday>.buildBirthdayStyle(context: Context, currYear: Int): NotificationCompat.Style {
    val inboxStyle = NotificationCompat.InboxStyle()

    inboxStyle.setBigContentTitle(context.getString(R.string.birthday_notification_title))
    for (birthday in this) {
        val years = currYear - birthday.birthdate.year
        inboxStyle.addLine(context.getString(R.string.birthday_years_old, birthday.name, years))
    }
    return inboxStyle
}

private fun createPendingIntent(context: Context, activityClass: Class<out Activity>): PendingIntent {
    // Define Activity to start
    val resultIntent = Intent(context, activityClass)
    val stackBuilder = TaskStackBuilder.create(context)
    // Adds the back stack
    stackBuilder.addParentStack(activityClass)
    // Adds the Intent to the top of the stack
    stackBuilder.addNextIntent(resultIntent)
    // Gets a PendingIntent containing the entire back stack
    return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
}

@Suppress("MagicNumber")
fun createBirthdayChannel(context: Context): NotificationManager {
    val channel = NotificationChannel(BIRTHDAY_CHANNEL_ID, BIRTHDAY_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
    channel.enableLights(true)
    channel.lightColor = Color.GREEN
    channel.enableVibration(true)
    channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
    return context.notificationManager.also { it.createNotificationChannel(channel) }
}
