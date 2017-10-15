package com.ternaryop.photoshelf.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// http://stackoverflow.com/questions/7344897/autostart-android-service
// adb shell am broadcast -a android.intent.action.BOOT_COMPLETED

public class BootServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            // don't slow the boot running immediately the service, wait 5 seconds
            AlarmBroadcastReceiver.createBirthdayAlarm(context, System.currentTimeMillis() + 5000);
            AlarmBroadcastReceiver.createExportAlarm(context, System.currentTimeMillis() + 15000);
        }
    }
}