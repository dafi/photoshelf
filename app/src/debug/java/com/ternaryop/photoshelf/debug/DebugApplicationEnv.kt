@file:Suppress("unused")
package com.ternaryop.photoshelf.debug

import android.content.Context
import androidx.preference.PreferenceManager
import com.github.scribejava.core.model.OAuth1AccessToken
import com.ternaryop.photoshelf.PhotoShelfApplicationEnv
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.core.prefs.PREF_PHOTOSHELF_APIKEY
import com.ternaryop.tumblr.android.TokenPreference

class DebugApplicationEnv : PhotoShelfApplicationEnv {
    override fun setup(context: Context) {
        ApiManager.updateToken(context.getString(R.string.PHOTOSHELF_ACCESS_TOKEN))
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(PREF_PHOTOSHELF_APIKEY, context.getString(R.string.PHOTOSHELF_ACCESS_TOKEN))
            .apply()

        val token = OAuth1AccessToken(
            context.getString(R.string.TUMBLR_OAUTH_TOKEN),
            context.getString(R.string.TUMBLR_OAUTH_TOKEN_SECRET))
        TokenPreference.from(context).storeAccessToken(token)
    }
}