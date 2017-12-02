package com.ternaryop.photoshelf.service;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Importer;
import com.ternaryop.photoshelf.dropbox.DropboxManager;
import com.ternaryop.photoshelf.util.log.Log;
import com.ternaryop.utils.DateTimeUtils;

public class AlarmBroadcastReceiver extends BroadcastReceiver {
    public static final String BIRTHDAY_ACTION = "birthday";
    public static final String EXPORT_ACTION = "export";

    @Override
    public void onReceive(Context context, Intent intent) {
        AppSupport appSupport = new AppSupport(context);
        if (BIRTHDAY_ACTION.equals(intent.getAction())) {
            if (!hasAlreadyNotifiedToday(appSupport)) {
                BirthdayUtils.notifyBirthday(appSupport);
            }
        } else if (EXPORT_ACTION.equals(intent.getAction())) {
            startExport(appSupport);
        }
    }

    public static void createBirthdayAlarm(Context context, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager)context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        final Intent serviceIntent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
        serviceIntent.setAction(BIRTHDAY_ACTION);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0,
                serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    public static void createExportAlarm(Context context, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager)context.getApplicationContext()
                .getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        final Intent serviceIntent = new Intent(context.getApplicationContext(), AlarmBroadcastReceiver.class);
        serviceIntent.setAction(EXPORT_ACTION);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(),
                0,
                serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // every 3 hours
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis, AlarmManager.INTERVAL_HOUR * 3, pendingIntent);
    }

    private void startExport(final AppSupport appSupport) {
        if (!appSupport.isAutomaticExportEnabled()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                Importer importer = new Importer(appSupport, DropboxManager.getInstance(appSupport));
                exportPosts(importer);
                exportBirthdays(importer);
                exportTotalUsers(importer);
            }

            private void exportTotalUsers(Importer importer) {
                final long exportDaysPeriod = appSupport.getExportDaysPeriod();
                final long lastUpdate = appSupport.getLastFollowersUpdateTime();
                if (lastUpdate < 0 || exportDaysPeriod <= DateTimeUtils.daysSinceTimestamp(lastUpdate)) {
                    try {
                        importer.syncExportTotalUsersToCSV(Importer.getTotalUsersPath(), appSupport.getSelectedBlogName());
                        appSupport.setLastFollowersUpdateTime(System.currentTimeMillis());
                    } catch (Exception e) {
                        Log.error(e, getLogPath(), "Export total users");
                    }
                }
            }

            private void exportBirthdays(Importer importer) {
                try {
                    importer.exportBirthdaysToCSV(Importer.getBirthdaysPath());
                } catch (Exception e) {
                    Log.error(e, getLogPath(), "Export birthdays");
                }
            }

            private void exportPosts(Importer importer) {
                try {
                    importer.exportPostsToCSV(Importer.getPostsPath());
                } catch (Exception e) {
                    Log.error(e, getLogPath(), "Export posts");
                }
            }
        }).start();
    }

    @NonNull
    private File getLogPath() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "export_errors.txt");
    }

    private boolean hasAlreadyNotifiedToday(Context context) {
        AppSupport appSupport = new AppSupport(context);
        GregorianCalendar lastBirthdayShowTime = new GregorianCalendar();
        lastBirthdayShowTime.setTimeInMillis(appSupport.getLastBirthdayShowTime());
        GregorianCalendar nowMS = new GregorianCalendar();
        if ((nowMS.get(Calendar.DAY_OF_MONTH) == lastBirthdayShowTime.get(Calendar.DAY_OF_MONTH))
            && (nowMS.get(Calendar.MONTH) == lastBirthdayShowTime.get(Calendar.MONTH))) {
            return true;
        }
        appSupport.setLastBirthdayShowTime(nowMS.getTimeInMillis());
        return false;
    }
}