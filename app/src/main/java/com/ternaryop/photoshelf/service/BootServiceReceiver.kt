package com.ternaryop.photoshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// https://stackoverflow.com/questions/11325920/how-to-test-boot-completed-broadcast-receiver-in-emulator#comment57079528_30407531
// adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.ternaryop.photoshelf.debug

const val BIRTHDAY_BOOT_ALARM_DELAY_MILLIS = 4 * 60 * 1000L
const val EXPORT_BOOT_ALARM_DELAY_MILLIS = 15000L

class BootServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // don't slow the boot running immediately the service, wait some amount of time before run
            val millis = System.currentTimeMillis()
            AlarmBroadcastReceiver.createBirthdayAlarm(context, millis + BIRTHDAY_BOOT_ALARM_DELAY_MILLIS)
            AlarmBroadcastReceiver.createExportAlarm(context, millis + EXPORT_BOOT_ALARM_DELAY_MILLIS)
        }
    }
}