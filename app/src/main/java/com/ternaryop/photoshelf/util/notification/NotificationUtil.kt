package com.ternaryop.photoshelf.util.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.TextUtils
import androidx.core.app.NotificationCompat
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.activity.BirthdaysPublisherActivity
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.utils.date.year
import com.ternaryop.utils.date.yearsBetweenDates
import com.ternaryop.utils.dialog.getExceptionMessageChain
import org.joda.time.LocalDate
import org.joda.time.Years
import java.text.DateFormat
import java.util.Calendar

/**
 * Created by dficano on 12/10/17.
 * Helper class for notification calls made by PhotoShelf
 */

private const val NOTIFICATION_ID = 1
const val NOTIFICATION_ID_IMPORT_BIRTHDAY = 2

fun NotificationCompat.Builder.setupMultiLineNotification(
    bigTitle: String, lines: List<String>, deleteIntent: PendingIntent? = null) {
    val inboxStyle = NotificationCompat.InboxStyle()
    inboxStyle.setBigContentTitle(bigTitle)
    for (line in lines) {
        inboxStyle.addLine(line)
    }
    setStyle(inboxStyle)
    setDeleteIntent(deleteIntent)
}

class NotificationUtil(context: Context) : ContextWrapper(context) {

    val notificationManager: NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val clearBirthdaysPendingIntent: PendingIntent
        get() {
            val intent = Intent(this, ClearBirthdayNotificationBroadcastReceiver::class.java)
            intent.action = BIRTHDAY_CLEAR_ACTION
            return PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

    init {
        createChannel()
    }

    fun notifyError(t: Throwable, title: String, ticker: String? = null, offsetId: Int = 0) {
        val builder = createNotification(title, ticker, null, R.drawable.stat_notify_error)
        builder.setupMultiLineNotification("Error", t.getExceptionMessageChain())
        // add offsetId to ensure every notification is shown
        notificationManager.notify(ERROR_TAG, NOTIFICATION_ID + offsetId, builder.build())
    }

    fun clearBirthdaysNotification() {
        birthdaysContentLines.clear()
    }

    fun notifyBirthdayAdded(name: String, birthday: Calendar) {
        val date = DateFormat.getDateInstance().format(birthday.time)
        val age = Years.yearsBetween(LocalDate(birthday), LocalDate()).years.toString()

        val notification = createBirthdayNotification(
                getString(R.string.name_with_date_age, name, date, age),
                getString(R.string.new_birthday_ticker, name), null,
                R.drawable.stat_notify_bday,
                clearBirthdaysPendingIntent)
        // if notification is already visible the user doesn't receive any visual feedback so we clear it
        notificationManager.cancel(BIRTHDAY_ADDED_TAG, NOTIFICATION_ID)
        notificationManager.notify(BIRTHDAY_ADDED_TAG, NOTIFICATION_ID, notification)
    }

    fun notifyTodayBirthdays(list: List<Birthday>, currYear: Int) {
        if (list.isEmpty()) {
            return
        }

        val builder = NotificationCompat.Builder(this, BIRTHDAY_CHANNEL_ID)
                .setContentIntent(createPendingIntent(this))
                .setSmallIcon(R.drawable.stat_notify_bday)
        if (list.size == 1) {
            val birthday = list[0]
            builder.setContentTitle(resources.getQuantityString(R.plurals.birthday_title, list.size))
            builder.setContentText(
                getString(R.string.birthday_years_old, birthday.name, birthday.birthdate.yearsBetweenDates()))
        } else {
            builder.setStyle(buildBirthdayStyle(list, currYear))
            // The text is shown when there isn't enough space for inboxStyle
            builder.setContentTitle(resources.getQuantityString(R.plurals.birthday_title, list.size, list.size))
            builder.setContentText(TextUtils.join(", ", getBirthdayNames(list)))
        }

        // remove notification when user clicks on it
        builder.setAutoCancel(true)

        notificationManager.notify(BIRTHDAY_TODAY_TAG, NOTIFICATION_ID, builder.build())
    }

    @Suppress("MagicNumber")
    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val channelName = "Birthdays"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(BIRTHDAY_CHANNEL_ID, channelName, importance)
        channel.enableLights(true)
        channel.lightColor = Color.RED
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager.createNotificationChannel(channel)
    }

    fun createNotification(contentText: String,
        stringTicker: String?, subText: String?, iconId: Int): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(this, BIRTHDAY_CHANNEL_ID)
                .setContentText(contentText)
                .setTicker(stringTicker)
                .setSmallIcon(iconId)
                .setAutoCancel(true) // remove notification when user clicks on it

        if (subText != null) {
            builder.setSubText(subText)
        }

        return builder
    }

    private fun createBirthdayNotification(contentText: String,
        stringTicker: String, subText: String?, iconId: Int, deleteIntent: PendingIntent): Notification {
        val builder = createNotification(contentText, stringTicker, subText, iconId)

        birthdaysContentLines.add(contentText)
        builder.setupMultiLineNotification(getString(R.string.birthdays_found), birthdaysContentLines, deleteIntent)
        builder.setNumber(birthdaysContentLines.size)

        return builder.build()
    }

    private fun getBirthdayNames(list: List<Birthday>): List<String> = list.map { it.name }

    private fun buildBirthdayStyle(list: List<Birthday>, currYear: Int): NotificationCompat.Style {
        val inboxStyle = NotificationCompat.InboxStyle()

        inboxStyle.setBigContentTitle(getString(R.string.birthday_notification_title))
        for (birthday in list) {
            val years = currYear - birthday.birthdate.year
            inboxStyle.addLine(getString(R.string.birthday_years_old, birthday.name, years))
        }
        return inboxStyle
    }

    class ClearBirthdayNotificationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BIRTHDAY_CLEAR_ACTION == intent.action) {
                NotificationUtil(context).clearBirthdaysNotification()
            }
        }
    }

    companion object {
        const val BIRTHDAY_CHANNEL_ID = "birthdayId"
        private const val BIRTHDAY_ADDED_TAG = "com.ternaryop.photoshelf.birthday.added"
        private const val BIRTHDAY_TODAY_TAG = "com.ternaryop.photoshelf.birthday.today"

        private const val BIRTHDAY_CLEAR_ACTION = "com.ternaryop.photoshelf.birthday.clear"

        private const val ERROR_TAG = "com.ternaryop.photoshelf.error"

        private val birthdaysContentLines = mutableListOf<String>()

        private fun createPendingIntent(context: Context): PendingIntent {
            // Define Activity to start
            val resultIntent = Intent(context, BirthdaysPublisherActivity::class.java)
            val stackBuilder = TaskStackBuilder.create(context)
            // Adds the back stack
            stackBuilder.addParentStack(BirthdaysPublisherActivity::class.java)
            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(resultIntent)
            // Gets a PendingIntent containing the entire back stack
            return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
