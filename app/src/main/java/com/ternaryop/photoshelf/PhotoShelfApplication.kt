@file:Suppress("unused")
package com.ternaryop.photoshelf

import android.app.Application
import com.ternaryop.feedly.FeedlyClient
import com.ternaryop.feedly.FeedlyClientInfo
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.customsearch.GoogleCustomSearchClient
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.util.okhttp3.OkHttpUtil
import com.ternaryop.utils.dropbox.DropboxManager

/**
 * Created by dave on 17/04/15.
 * Make extra global init
 */
class PhotoShelfApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        TumblrManager.setup(
            getString(R.string.TUMBLR_CONSUMER_KEY),
            getString(R.string.TUMBLR_CONSUMER_SECRET),
            getString(R.string.TUMBLR_CALLBACK_URL))
        DropboxManager.setup(getString(R.string.DROPBOX_APP_KEY), resources.getString(R.string.dropbox_client_identifier))
        val okHttpClient = if (BuildConfig.DEBUG) OkHttpUtil.debugHttpClient() else null
        ApiManager
            .setup(AppSupport(this)
            .photoShelfApikey, BuildConfig.PHOTOSHELF_API_PREFIX, okHttpClient)
        FeedlyClient.setup(FeedlyClientInfo(
            getString(R.string.FEEDLY_USER_ID),
            getString(R.string.FEEDLY_REFRESH_TOKEN),
            getString(R.string.FEEDLY_CLIENT_ID),
            getString(R.string.FEEDLY_CLIENT_SECRET)), okHttpClient)
        GoogleCustomSearchClient.setup(
            getString(R.string.GOOGLE_CSE_APIKEY),
            getString(R.string.GOOGLE_CSE_CX),
            okHttpClient)
    }
}
