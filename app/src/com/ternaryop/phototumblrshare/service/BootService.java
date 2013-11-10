package com.ternaryop.phototumblrshare.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.ternaryop.phototumblrshare.birthday.BirthdayUtils;

public class BootService extends Service {
	public static final String BIRTHDAY_ACTION = "birthday";

	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (BIRTHDAY_ACTION.equals(intent.getAction())) {
			if (!hasAlreadyNotifiedToday()) {
				BirthdayUtils.notifyBirthday(getApplicationContext());
			}
		}
		return START_NOT_STICKY;
	}

	private boolean hasAlreadyNotifiedToday() {
//		AppSupport appSupport = new AppSupport(getApplicationContext());
//		GregorianCalendar lastBirthdayShowTime = new GregorianCalendar();
//		lastBirthdayShowTime.setTimeInMillis(appSupport.getLastBirthdayShowTime());
//		GregorianCalendar nowMS = new GregorianCalendar();
//		if ((nowMS.get(Calendar.DAY_OF_MONTH) == lastBirthdayShowTime.get(Calendar.DAY_OF_MONTH))
//			&& (nowMS.get(Calendar.MONTH) == lastBirthdayShowTime.get(Calendar.MONTH))) {
//			return true;
//		}
//		appSupport.setLastBirthdayShowTime(nowMS.getTimeInMillis());
		return false;
	}

	@Override
	public void onDestroy() {
	}
}