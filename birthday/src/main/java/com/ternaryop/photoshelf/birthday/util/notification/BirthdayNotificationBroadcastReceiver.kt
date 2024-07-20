package com.ternaryop.photoshelf.birthday.util.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.util.notification.setupMultiLineNotification
import com.ternaryop.utils.date.yearsBetweenDates
import java.text.DateFormat
import java.util.Calendar

class BirthdayNotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (birthdayClearAction == intent.action) {
            contentLines.clear()
        }
    }

    companion object {
        private const val birthdayClearAction = "com.ternaryop.photoshelf.birthday.clear"
        private const val birthdayAddedTag = "com.ternaryop.photoshelf.birthday.added"

        private val contentLines = mutableListOf<String>()

        private fun clearBirthdays(context: Context): PendingIntent {
            val intent = Intent(context, BirthdayNotificationBroadcastReceiver::class.java)
                .setAction(birthdayClearAction)
            return PendingIntent.getBroadcast(
                context.applicationContext,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun notifyBirthdayAdded(context: Context, name: String, birthdate: Calendar) {
            val date = DateFormat.getDateInstance().format(birthdate.time)
            val age = birthdate.yearsBetweenDates().toString()
            val clearIntent = clearBirthdays(context)
            val contentText = context.getString(R.string.name_with_date_age, name, date, age)

            contentLines.add(contentText)

            val notification = NotificationCompat.Builder(context, BIRTHDAY_CHANNEL_ID)
                .setContentText(contentText)
                .setTicker(context.getString(R.string.new_birthday_ticker, name))
                .setSmallIcon(R.drawable.stat_notify_bday)
                .setAutoCancel(true) // remove notification when user clicks on it
                .setupMultiLineNotification(context.getString(R.string.birthdays_found), contentLines, clearIntent)
                .setNumber(contentLines.size)
                .build()

            createBirthdayChannel(context).run {
                // if notification is already visible the user doesn't receive any visual feedback so we clear it
                cancel(birthdayAddedTag, BIRTHDAY_NOTIFICATION_ID)
                notify(birthdayAddedTag, BIRTHDAY_NOTIFICATION_ID, notification)
            }
        }
    }
}
