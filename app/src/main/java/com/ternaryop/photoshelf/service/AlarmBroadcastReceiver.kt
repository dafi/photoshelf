package com.ternaryop.photoshelf.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.importer.BatchExporter
import com.ternaryop.photoshelf.util.date.dayOfMonth
import com.ternaryop.photoshelf.util.date.month
import java.util.Calendar

class AlarmBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appSupport = AppSupport(context)
        when (intent.action) {
            ACTION_BIRTHDAY -> if (!hasAlreadyNotifiedToday(appSupport)) BirthdayUtils.notifyBirthday(appSupport)
            ACTION_EXPORT -> startExport(appSupport)
        }
    }

    private fun startExport(appSupport: AppSupport) {
        if (appSupport.isAutomaticExportEnabled) {
            Thread({ BatchExporter(appSupport).export() }).start()
        }
    }

    private fun hasAlreadyNotifiedToday(context: Context): Boolean {
        val appSupport = AppSupport(context)
        val lastBirthdayShowTime = Calendar.getInstance()
        lastBirthdayShowTime.timeInMillis = appSupport.lastBirthdayShowTime
        val nowMS = Calendar.getInstance()
        if (nowMS.dayOfMonth == lastBirthdayShowTime.dayOfMonth && nowMS.month == lastBirthdayShowTime.month) {
            return true
        }
        appSupport.lastBirthdayShowTime = nowMS.timeInMillis
        return false
    }

    companion object {
        const val ACTION_BIRTHDAY = "birthday"
        const val ACTION_EXPORT = "export"
        private const val EXPORT_INTERVAL = AlarmManager.INTERVAL_HOUR * 3

        fun createBirthdayAlarm(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.applicationContext
                    .getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val serviceIntent = Intent(context.applicationContext, AlarmBroadcastReceiver::class.java)
            serviceIntent.action = ACTION_BIRTHDAY

            val pendingIntent = PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    serviceIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT)

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                triggerAtMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }

        fun createExportAlarm(context: Context, triggerAtMillis: Long, intervalMillis: Long = EXPORT_INTERVAL) {
            val alarmManager = context.applicationContext
                    .getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val serviceIntent = Intent(context.applicationContext, AlarmBroadcastReceiver::class.java)
            serviceIntent.action = ACTION_EXPORT

            val pendingIntent = PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    serviceIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT)

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, intervalMillis, pendingIntent)
        }
    }
}