package com.ternaryop.utils.dropbox

import android.content.Context
import androidx.fragment.app.Fragment
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxSdkVersion
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.DbxClientV2

/**
 * Created by dave on 25/04/15.
 * Wrap dropbox session to handle token storing and cleanup
 */

private const val DROPBOX_ACCESS_KEY_NAME = "dropboxAccessKey"
private const val DROPBOX_ACCESS_SECRET_NAME = "dropboxAccessSecretName"
private const val DROPBOX_ACCOUNT_PREFS_NAME = "dropboxAccount"

class DropboxManager private constructor(context: Context, val clientIdentifier: String) {

    private val preferences = context.getSharedPreferences(DROPBOX_ACCOUNT_PREFS_NAME, 0)

    private var dbxClientV2: DbxClientV2? = null

    val client: DbxClientV2?
        get() {
            val accessToken = preferences.getString(DROPBOX_ACCESS_SECRET_NAME, null) ?: return null
            if (dbxClientV2 == null) {
                val config = DbxRequestConfig.newBuilder(clientIdentifier).build()
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

    fun startOAuth2Authentication(fragment: Fragment) {
        Auth.startOAuth2Authentication(fragment.requireActivity(), appKey)
    }

    companion object {
        private var instance: DropboxManager? = null
        private var clientIdentifier = ""

        val Version = DbxSdkVersion.Version ?: "N/A"

        private var appKey = ""

        fun setup(appKey: String, clientIdentifier: String) {
            Companion.appKey = appKey
            Companion.clientIdentifier = clientIdentifier
            instance = null
        }

        fun getInstance(context: Context): DropboxManager {
            val currentInstance = instance

            if (currentInstance != null) {
                return currentInstance
            }

            return synchronized(DropboxManager::class.java) {
                var newInstance = instance
                if (newInstance == null) {
                    newInstance = DropboxManager(context, clientIdentifier)
                    instance = newInstance
                }
                newInstance
            }
        }
    }
}
