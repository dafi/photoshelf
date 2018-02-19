package com.ternaryop.photoshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// http://stackoverflow.com/questions/7344897/autostart-android-service
// adb shell am broadcast -a android.intent.action.BOOT_COMPLETED

const val BIRTHDAY_BOOT_ALARM_DELAY_MILLIS = 5000L
const val EXPORT_BOOT_ALARM_DELAY_MILLIS = 15000L

class BootServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // don't slow the boot running immediately the service, wait 5 seconds
            AlarmBroadcastReceiver.createBirthdayAlarm(context, System.currentTimeMillis() + BIRTHDAY_BOOT_ALARM_DELAY_MILLIS)
            AlarmBroadcastReceiver.createExportAlarm(context, System.currentTimeMillis() + EXPORT_BOOT_ALARM_DELAY_MILLIS)
        }
    }
}