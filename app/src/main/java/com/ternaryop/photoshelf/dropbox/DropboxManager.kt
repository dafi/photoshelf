package com.ternaryop.photoshelf.dropbox

import android.app.Fragment
import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxSdkVersion
import com.dropbox.core.android.Auth
import com.dropbox.core.android.AuthActivity
import com.dropbox.core.v2.DbxClientV2
import com.ternaryop.photoshelf.R

/**
 * Created by dave on 25/04/15.
 * Wrap dropbox session to handle token storing and cleanup
 */

private const val DROPBOX_ACCESS_KEY_NAME = "dropboxAccessKey"
private const val DROPBOX_ACCESS_SECRET_NAME = "dropboxAccessSecretName"
private const val DROPBOX_ACCOUNT_PREFS_NAME = "dropboxAccount"

class DropboxManager private constructor(context: Context) {

    private val preferences = context.getSharedPreferences(DROPBOX_ACCOUNT_PREFS_NAME, 0)

    private val appKey = context.getString(R.string.DROPBOX_APP_KEY)
    private var dbxClientV2: DbxClientV2? = null

    val client: DbxClientV2?
        get() {
            val accessToken = preferences.getString(DROPBOX_ACCESS_SECRET_NAME, null) ?: return null
            if (dbxClientV2 == null) {
                val config = DbxRequestConfig.newBuilder("photoshelf-android/1.0").build()
                dbxClientV2 = DbxClientV2(config, accessToken)
            }
            return dbxClientV2
        }

    val isLinked: Boolean
        get() = preferences.contains(DROPBOX_ACCESS_SECRET_NAME)

    private fun storeAuth() {
        // Store the OAuth 2 access token, if there is one.
        val oauth2AccessToken = Auth.getOAuth2Token()
        if (oauth2AccessToken != null) {
            val edit = preferences.edit()
            edit.putString(DROPBOX_ACCESS_KEY_NAME, "oauth2:")
            edit.putString(DROPBOX_ACCESS_SECRET_NAME, oauth2AccessToken)
            edit.apply()
        }
    }

    private fun clearKeys() {
        val edit = preferences.edit()
        edit.clear()
        edit.apply()
    }

    @Throws(IllegalStateException::class)
    fun finishAuthentication(): String? {
        storeAuth()
        return preferences.getString(DROPBOX_ACCESS_SECRET_NAME, null)
    }

    fun unlink() {
        clearKeys()
    }

    fun startOAuth2AuthenticationForResult(fragment: Fragment, requestCode: Int) {
        if (!/*alertUser*/AuthActivity.checkAppBeforeAuth(fragment.activity, appKey, true)) {
            return
        }

        val intent = AuthActivity.makeIntent(fragment.activity, appKey, null, null)
        fragment.startActivityForResult(intent, requestCode)
    }

    companion object {
        private var instance: DropboxManager? = null

        val Version = DbxSdkVersion.Version!!

        fun getInstance(context: Context): DropboxManager {
            if (instance == null) {
                synchronized(DropboxManager::class.java) {
                    if (instance == null) {
                        instance = DropboxManager(context)
                    }
                }
            }
            return instance!!
        }
    }
}
