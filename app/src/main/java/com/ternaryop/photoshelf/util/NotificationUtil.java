package com.ternaryop.photoshelf.util;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.activity.BirthdaysPublisherActivity;
import com.ternaryop.photoshelf.db.Birthday;
import org.joda.time.LocalDate;
import org.joda.time.Years;

/**
 * Created by dficano on 12/10/17.
 * Helper class for notification calls made by PhotoShelf
 */

public class NotificationUtil extends ContextWrapper {
    public static final String BIRTHDAY_CHANNEL_ID = "birthdayId";
    private static final String BIRTHDAY_ADDED_TAG = "com.ternaryop.photoshelf.birthday.added";
    private static final String BIRTHDAY_TODAY_TAG = "com.ternaryop.photoshelf.birthday.today";
    private static final String EXPORT_TAG = "com.ternaryop.photoshelf.export";

    private static final String BIRTHDAY_CLEAR_ACTION = "com.ternaryop.photoshelf.birthday.clear";

    private static final String ERROR_TAG = "com.ternaryop.photoshelf.error";

    private static final int NOTIFICATION_ID = 1;
    public static final int NOTIFICATION_ID_IMPORT_BIRTHDAY = 2;
    public static final int NOTIFICATION_ID_IMPORT_POSTS = 3;

    private static final List<String> birthdaysContentLines = new ArrayList<>();

    private NotificationManager notificationManager;

    public NotificationUtil(Context context) {
        super(context);
        createChannel();
    }

    public void notifyError(Throwable t, String title, String ticker) {
        notifyError(t, title, ticker, 0);
    }

    public void notifyError(Throwable t, String title, String ticker, int offsetId) {
        Notification notification = createNotification(title, ticker, t.getLocalizedMessage(), R.drawable.stat_notify_error).build();
        // add offsetId to ensure every notification is shown
        getNotificationManager().notify(ERROR_TAG, NOTIFICATION_ID + offsetId, notification);
    }

    public void clearBirthdaysNotification() {
        birthdaysContentLines.clear();
    }

    public void notifyBirthdayAdded(String name, Date birthday) {
        String date = DateFormat.getDateInstance().format(birthday);
        String age = String.valueOf(Years.yearsBetween(new LocalDate(birthday), new LocalDate()).getYears());

        Notification notification = createBirthdayNotification(
                getString(R.string.name_with_date_age, name, date, age),
                getString(R.string.new_birthday_ticker, name),
                null,
                R.drawable.stat_notify_bday,
                getClearBirthdaysPendingIntent());
        // if notification is already visible the user doesn't receive any visual feedback so we clear it
        getNotificationManager().cancel(BIRTHDAY_ADDED_TAG, NOTIFICATION_ID);
        getNotificationManager().notify(BIRTHDAY_ADDED_TAG, NOTIFICATION_ID, notification);
    }

    public void notifyTodayBirthdays(List<Birthday> list, int currYear) {
        if (list.isEmpty()) {
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BIRTHDAY_CHANNEL_ID)
                .setContentIntent(createPendingIntent(this))
                .setSmallIcon(R.drawable.stat_notify_bday);
        if (list.size() == 1) {
            Birthday birthday = list.get(0);
            builder.setContentTitle(getResources().getQuantityString(R.plurals.birthday_title, list.size()));
            int years = currYear - birthday.getBirthDateCalendar().get(Calendar.YEAR);
            builder.setContentText(getString(R.string.birthday_years_old, birthday.getName(), years));
        } else {
            builder.setStyle(buildBirthdayStyle(list, currYear));
            // The text is shown when there isn't enough space for inboxStyle
            builder.setContentTitle(getResources().getQuantityString(R.plurals.birthday_title, list.size(), list.size()));
            builder.setContentText(TextUtils.join(", ", getBithdayNames(list)));
        }

        // remove notification when user clicks on it
        builder.setAutoCancel(true);

        getNotificationManager().notify(BIRTHDAY_TODAY_TAG, NOTIFICATION_ID, builder.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        CharSequence channelName = "Birthdays";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(BIRTHDAY_CHANNEL_ID, channelName, importance);
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        notificationManager.createNotificationChannel(channel);
    }

    public NotificationCompat.Builder createNotification(String contentText, String stringTicker, String subText, int iconId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BIRTHDAY_CHANNEL_ID)
                .setContentText(contentText)
                .setTicker(stringTicker)
                .setSmallIcon(iconId)
                .setAutoCancel(true); // remove notification when user clicks on it

        if (subText != null) {
            builder.setSubText(subText);
        }

        return builder;
    }

    private Notification createBirthdayNotification(String contentText, String stringTicker, String subText, int iconId, PendingIntent deleteIntent) {
        NotificationCompat.Builder builder = createNotification(contentText, stringTicker, subText, iconId);

        birthdaysContentLines.add(contentText);
        setupBirthdayNotification(builder, deleteIntent);

        return builder.build();
    }

    private void setupBirthdayNotification(NotificationCompat.Builder builder, PendingIntent deleteIntent) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(getString(R.string.birthdays_found));
        for (String line : birthdaysContentLines) {
            inboxStyle.addLine(line);
        }
        builder.setStyle(inboxStyle);
        builder.setDeleteIntent(deleteIntent);
        builder.setNumber(birthdaysContentLines.size());
    }

    @NonNull
    private ArrayList<String> getBithdayNames(List<Birthday> list) {
        ArrayList<String> names = new ArrayList<>();
        for (Birthday birthday : list) {
            names.add(birthday.getName());
        }
        return names;
    }

    private NotificationCompat.Style buildBirthdayStyle(List<Birthday> list, int currYear) {
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        Calendar cal = Calendar.getInstance(Locale.US);

        inboxStyle.setBigContentTitle(getString(R.string.birthday_notification_title));
        for (Birthday birthday : list) {
            cal.setTime(birthday.getBirthDate());
            int years = currYear - cal.get(Calendar.YEAR);
            inboxStyle.addLine(getString(R.string.birthday_years_old, birthday.getName(), years));
        }
        return inboxStyle;
    }

    private static PendingIntent createPendingIntent(Context context) {
        // Define Activity to start
        Intent resultIntent = new Intent(context, BirthdaysPublisherActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack
        stackBuilder.addParentStack(BirthdaysPublisherActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public NotificationManager getNotificationManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;

    }

    public static class ClearBirthdayNotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BIRTHDAY_CLEAR_ACTION.equals(intent.getAction())) {
                new NotificationUtil(context).clearBirthdaysNotification();
            }
        }
    }

    private PendingIntent getClearBirthdaysPendingIntent() {
        Intent intent = new Intent(this, ClearBirthdayNotificationBroadcastReceiver.class);
        intent.setAction(BIRTHDAY_CLEAR_ACTION);
        return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void notifyExport(String contentText, String ticker, String subText, int iconId) {
        getNotificationManager().notify(EXPORT_TAG, 0, createNotification(contentText, ticker, subText, iconId).build());
    }
}
