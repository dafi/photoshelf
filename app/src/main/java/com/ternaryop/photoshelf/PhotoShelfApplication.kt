package com.ternaryop.photoshelf

import android.app.Application

import com.ternaryop.utils.reactivex.UndeliverableErrorHandler
import io.reactivex.plugins.RxJavaPlugins
import net.danlew.android.joda.JodaTimeAndroid

/**
 * Created by dave on 17/04/15.
 * Make extra global init
 */
class PhotoShelfApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // It's important to initialize the ResourceZoneInfoProvider; otherwise
        // joda-time-android will not work.
        JodaTimeAndroid.init(this)
        RxJavaPlugins.setErrorHandler(UndeliverableErrorHandler())
    }
}
