package com.ternaryop.photoshelf;

import android.app.Application;

import net.danlew.android.joda.JodaTimeAndroid;


/**
 * Created by dave on 17/04/15.
 * Make extra global init
 */
public class PhotoShelfApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // It's important to initialize the ResourceZoneInfoProvider; otherwise
        // joda-time-android will not work.
        JodaTimeAndroid.init(this);
    }
}
