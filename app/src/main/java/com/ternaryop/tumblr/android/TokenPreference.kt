package com.ternaryop.tumblr.android

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuth1RequestToken
import com.github.scribejava.core.model.OAuth1Token

class TokenPreference(val context: Context) {
    val requestToken: OAuth1RequestToken
        get() = getRequestToken(context.getSharedPreferences(PREFS_NAME, 0))
    val accessToken: OAuth1AccessToken
        get() = getAccessToken(PreferenceManager.getDefaultSharedPreferences(context))
    val isAccessTokenValid: Boolean
        get() {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            return preferences.contains(PREF_OAUTH_TOKEN) && preferences.contains(PREF_OAUTH_SECRET)
        }

    fun storeRequestToken(requestToken: OAuth1RequestToken) {
        storeToken(context.getSharedPreferences(PREFS_NAME, 0), requestToken)
    }

    fun storeAccessToken(accessToken: OAuth1AccessToken) {
        storeToken(PreferenceManager.getDefaultSharedPreferences(context), accessToken)
    }

    fun clearAccessToken() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val edit = preferences.edit()
        edit.remove(PREF_OAUTH_TOKEN)
        edit.remove(PREF_OAUTH_SECRET)
        edit.apply()
    }

    companion object {
        private const val PREFS_NAME = "tumblr"
        private const val PREF_OAUTH_SECRET = "oAuthSecret"
        private const val PREF_OAUTH_TOKEN = "oAuthToken"

        fun from(context: Context): TokenPreference {
            return TokenPreference(context)
        }

        private fun getRequestToken(sharedPreferences: SharedPreferences): OAuth1RequestToken {
            return OAuth1RequestToken(
                sharedPreferences.getString(PREF_OAUTH_TOKEN, null),
                sharedPreferences.getString(PREF_OAUTH_SECRET, null))
        }

        private fun getAccessToken(sharedPreferences: SharedPreferences): OAuth1AccessToken {
            return OAuth1AccessToken(
                sharedPreferences.getString(PREF_OAUTH_TOKEN, null),
                sharedPreferences.getString(PREF_OAUTH_SECRET, null))
        }

        private fun storeToken(sharedPreferences: SharedPreferences, token: OAuth1Token) {
            sharedPreferences
                .edit()
                .putString(PREF_OAUTH_TOKEN, token.token)
                .putString(PREF_OAUTH_SECRET, token.tokenSecret)
                .apply()
        }
    }
}