package com.ternaryop.photoshelf

import android.app.Application
import com.ternaryop.feedly.FeedlyClient
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.customsearch.GoogleCustomSearchClient
import com.ternaryop.util.okhttp3.OkHttpUtil
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.utils.dropbox.DropboxManager
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
        DropboxManager.setup(getString(R.string.DROPBOX_APP_KEY), resources.getString(R.string.dropbox_client_identifier))
        val okHttpClient = if (BuildConfig.DEBUG) OkHttpUtil.debugHttpClient() else null
        ApiManager
            .setup(AppSupport(this)
            .photoShelfApikey, BuildConfig.PHOTOSHELF_API_PREFIX, okHttpClient)
        FeedlyClient.setup(okHttpClient)
        GoogleCustomSearchClient.setup(
            getString(R.string.GOOGLE_CSE_APIKEY),
            getString(R.string.GOOGLE_CSE_CX),
            okHttpClient)
    }
}
