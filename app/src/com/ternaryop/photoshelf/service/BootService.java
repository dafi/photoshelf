package com.ternaryop.photoshelf.service;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Importer;

public class BootService extends Service {
    public static final String BIRTHDAY_ACTION = "birthday";
    public static final String EXPORT_ACTION = "export";
    private AppSupport appSupport;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        appSupport = new AppSupport(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BIRTHDAY_ACTION.equals(intent.getAction())) {
            if (!hasAlreadyNotifiedToday()) {
                BirthdayUtils.notifyBirthday(getApplicationContext());
            }
        } else if (EXPORT_ACTION.equals(intent.getAction())) {
            startExport();
        }
        return START_NOT_STICKY;
    }

    private void startExport() {
        if (!appSupport.isAutomaticExportEnabled()) {
            return;
        }
        Importer importer = new Importer(getApplicationContext(), appSupport.getDbxAccountManager());
        try {
            importer.syncExportPostsToCSV(Importer.getPostsPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            importer.syncExportBirthdaysToCSV(Importer.getBirthdaysPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean hasAlreadyNotifiedToday() {
        AppSupport appSupport = new AppSupport(getApplicationContext());
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

    @Override
    public void onDestroy() {
    }
}