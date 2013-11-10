package com.ternaryop.phototumblrshare.service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// http://stackoverflow.com/questions/7344897/autostart-android-service
// adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -c android.intent.category.HOME -n com.ternaryop.phototumblrshare.service/BootServiceReceiver 

public class BootServiceReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Intent serviceIntent = new Intent(context.getApplicationContext(), BootService.class);
			serviceIntent.setAction(BootService.BIRTHDAY_ACTION);
			context.startService(serviceIntent);
		}
	}
}