package com.ternaryop.phototumblrshare.service;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// http://stackoverflow.com/questions/7344897/autostart-android-service
// adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -c android.intent.category.HOME -n com.ternaryop.phototumblrshare.service/BootServiceReceiver 

public class BootServiceReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			// don't slow the boot running immediately the service, wait 5 seconds
			createAlarm(context, Calendar.getInstance().getTimeInMillis() + 5000);
		}
	}

	private final void createAlarm(Context context, long triggerAtMillis) {
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
}