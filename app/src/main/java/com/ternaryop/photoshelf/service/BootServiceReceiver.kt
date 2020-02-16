package com.ternaryop.photoshelf.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// https://stackoverflow.com/questions/11325920/how-to-test-boot-completed-broadcast-receiver-in-emulator#comment57079528_30407531
// adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p com.ternaryop.photoshelf.debug

class BootServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            PhotoShelfJobService.scheduleBirthday(context)
            PhotoShelfJobService.scheduleExport(context)
        }
    }
}
