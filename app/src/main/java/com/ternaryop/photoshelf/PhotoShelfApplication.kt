package com.ternaryop.photoshelf

import android.app.Application
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.customsearch.GoogleCustomSearchClient
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.tumblr.android.TumblrManager
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
        TumblrManager.setup(
            getString(R.string.TUMBLR_CONSUMER_KEY),
            getString(R.string.TUMBLR_CONSUMER_SECRET),
            getString(R.string.TUMBLR_CALLBACK_URL))
        DropboxManager.setup(getString(R.string.DROPBOX_APP_KEY))
        ApiManager.setup(AppSupport(this).photoShelfApikey)
        GoogleCustomSearchClient.setup(
            getString(R.string.GOOGLE_CSE_APIKEY),
            getString(R.string.GOOGLE_CSE_CX))
    }
}
