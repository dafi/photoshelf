package com.ternaryop.photoshelf.service;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
            createBirthdayAlarm(context, Calendar.getInstance().getTimeInMillis() + 5000);
            createExportAlarm(context, Calendar.getInstance().getTimeInMillis() + 15000);
		}
	}

	private final void createBirthdayAlarm(Context context, long triggerAtMillis) {
		final Intent serviceIntent = new Intent(context.getApplicationContext(), BootService.class);
		serviceIntent.setAction(BootService.BIRTHDAY_ACTION);

        PendingIntent pendingIntent = PendingIntent.getService(context.getApplicationContext(),
        		0,
        		serviceIntent,
        		PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager)context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private final void createExportAlarm(Context context, long triggerAtMillis) {
        final Intent serviceIntent = new Intent(context.getApplicationContext(), BootService.class);
        serviceIntent.setAction(BootService.EXPORT_ACTION);

        PendingIntent pendingIntent = PendingIntent.getService(context.getApplicationContext(),
                0,
                serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager)context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        // every 3 hours
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_HOUR * 3, pendingIntent);
    }
}