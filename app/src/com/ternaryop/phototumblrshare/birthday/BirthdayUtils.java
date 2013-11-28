package com.ternaryop.phototumblrshare.birthday;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.activity.BirthdaysActivity;
import com.ternaryop.phototumblrshare.db.Birthday;
import com.ternaryop.phototumblrshare.db.BirthdayDAO;
import com.ternaryop.phototumblrshare.db.DBHelper;

public class BirthdayUtils {
	private static final String BIRTHDAY_NOTIFICATION_TAG = "com.ternaryop.photoshare.bday";
	private static final int BIRTHDAY_NOTIFICATION_ID = 1;

	public static boolean notifyBirthday(Context context) {
		BirthdayDAO birthdayDatabaseHelper = DBHelper
				.getInstance(context.getApplicationContext())
				.getBirthdayDAO();
		Calendar now = Calendar.getInstance(Locale.US);
		List<Birthday> list = birthdayDatabaseHelper.getBirthdayByDate(now.getTime());
		if (list.isEmpty()) {
			return false;
		}

		long currYear = now.get(Calendar.YEAR);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context.getApplicationContext())
				.setContentIntent(createPendingIntent(context))
				.setSmallIcon(R.drawable.stat_notify_bday);
		if (list.size() == 1) {
			Birthday birthday = list.get(0);
			builder.setContentTitle(context.getString(R.string.birthday_title_singular));
			Calendar cal = Calendar.getInstance(Locale.US);
			cal.setTime(birthday.getBirthDate());
			long years = currYear - cal.get(Calendar.YEAR);
			builder.setContentText(context.getString(R.string.birthday_years_old, birthday.getName(), years));
		} else {
			builder.setContentTitle(context.getString(R.string.birthday_title_plural, list.size()));
			NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
			inboxStyle.setBigContentTitle(context.getString(R.string.birthday_notification_title));
			for (Birthday birthday : list) {
				Calendar cal = Calendar.getInstance(Locale.US);
				cal.setTime(birthday.getBirthDate());
				long years = currYear - cal.get(Calendar.YEAR);
			    inboxStyle.addLine(context.getString(R.string.birthday_years_old, birthday.getName(), years));
			}
			builder.setStyle(inboxStyle);
		}

		Notification notification = builder.build();
		NotificationManager notificationManager = (NotificationManager)context.getApplicationContext()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(BIRTHDAY_NOTIFICATION_TAG, BIRTHDAY_NOTIFICATION_ID, notification);

		return true;
	}

	private static PendingIntent createPendingIntent(Context context) {
		// Define Activity to start
		Intent resultIntent = new Intent(context, BirthdaysActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
		// Adds the back stack
		stackBuilder.addParentStack(BirthdaysActivity.class);
		// Adds the Intent to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		// Gets a PendingIntent containing the entire back stack
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
		return resultPendingIntent;
	}
}
