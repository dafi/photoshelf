package com.ternaryop.photoshelf.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.photoshelf.util.date.dayOfMonth
import com.ternaryop.photoshelf.util.date.month
import com.ternaryop.photoshelf.util.log.Log
import com.ternaryop.utils.DateTimeUtils
import java.io.File
import java.util.Calendar

class AlarmBroadcastReceiver : BroadcastReceiver() {

    private val logPath: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "export_errors.txt")

    override fun onReceive(context: Context, intent: Intent) {
        val appSupport = AppSupport(context)
        if (BIRTHDAY_ACTION == intent.action) {
            if (!hasAlreadyNotifiedToday(appSupport)) {
                BirthdayUtils.notifyBirthday(appSupport)
            }
        } else if (EXPORT_ACTION == intent.action) {
            startExport(appSupport)
        }
    }

    private fun startExport(appSupport: AppSupport) {
        if (!appSupport.isAutomaticExportEnabled) {
            return
        }
        Thread(object : Runnable {
            override fun run() {
                val importer = Importer(appSupport, DropboxManager.getInstance(appSupport))
                exportPosts(importer)
                exportBirthdays(importer)
                exportTotalUsers(importer)
            }

            private fun exportTotalUsers(importer: Importer) {
                val exportDaysPeriod = appSupport.exportDaysPeriod.toLong()
                val lastUpdate = appSupport.lastFollowersUpdateTime
                if (lastUpdate < 0 || exportDaysPeriod <= DateTimeUtils.daysSinceTimestamp(lastUpdate)) {
                    try {
                        importer.syncExportTotalUsersToCSV(Importer.totalUsersPath, appSupport.selectedBlogName!!)
                        appSupport.lastFollowersUpdateTime = System.currentTimeMillis()
                    } catch (e: Exception) {
                        Log.error(e, logPath, "Export total users")
                    }
                }
            }

            private fun exportBirthdays(importer: Importer) {
                try {
                    importer.exportBirthdaysToCSV(Importer.birthdaysPath)
                } catch (e: Exception) {
                    Log.error(e, logPath, "Export birthdays")
                }
            }

            private fun exportPosts(importer: Importer) {
                try {
                    importer.exportPostsToCSV(Importer.postsPath)
                } catch (e: Exception) {
                    Log.error(e, logPath, "Export posts")
                }
            }
        }).start()
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
        const val BIRTHDAY_ACTION = "birthday"
        const val EXPORT_ACTION = "export"

        fun createBirthdayAlarm(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.applicationContext
                    .getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val serviceIntent = Intent(context.applicationContext, AlarmBroadcastReceiver::class.java)
            serviceIntent.action = BIRTHDAY_ACTION

            val pendingIntent = PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    serviceIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT)

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
        }

        fun createExportAlarm(context: Context, triggerAtMillis: Long) {
            val alarmManager = context.applicationContext
                    .getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val serviceIntent = Intent(context.applicationContext, AlarmBroadcastReceiver::class.java)
            serviceIntent.action = EXPORT_ACTION

            val pendingIntent = PendingIntent.getBroadcast(context.applicationContext,
                    0,
                    serviceIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT)

            // every 3 hours
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_HOUR * 3, pendingIntent)
        }
    }
}