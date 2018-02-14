package com.ternaryop.tumblr

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.AsyncTask
import android.preference.PreferenceManager
import com.ternaryop.photoshelf.R
import com.ternaryop.utils.JSONUtils
import org.json.JSONException
import org.json.JSONObject
import org.scribe.builder.ServiceBuilder
import org.scribe.builder.api.TumblrApi
import org.scribe.model.OAuthConstants
import org.scribe.model.OAuthRequest
import org.scribe.model.Response
import org.scribe.model.Token
import org.scribe.model.Verb
import org.scribe.model.Verifier
import org.scribe.oauth.OAuthService
import java.io.IOException

class TumblrHttpOAuthConsumer(context: Context) {
    private val oAuthService: OAuthService
    private val accessToken: Token
    val consumerKey = context.getString(R.string.CONSUMER_KEY)!!

    init {

        oAuthService = ServiceBuilder()
                .provider(TumblrApi::class.java)
                .apiKey(consumerKey)
                .apiSecret(context.getString(R.string.CONSUMER_SECRET))
                .callback(context.getString(R.string.CALLBACK_URL))
                .build()

        accessToken = TokenPreference.from(context).accessToken
    }

    @Throws(IOException::class)
    private fun getSignedPostResponse(url: String, params: Map<String, *>): Response {
        val oAuthReq = OAuthRequest(Verb.POST, url)

        for (key in params.keys) {
            val value = params[key]
            if (value is String) {
                oAuthReq.addBodyParameter(key, value)
            }
        }
        oAuthService.signRequest(accessToken, oAuthReq)
        return MultipartConverter(oAuthReq, params).request.send()
    }

    private fun getSignedGetResponse(url: String, params: Map<String, *>?): Response {
        val oAuthReq = OAuthRequest(Verb.GET, url)

        if (params != null) {
            for (key in params.keys) {
                val value = params[key]
                oAuthReq.addQuerystringParameter(key, value.toString())
            }
        }
        oAuthService.signRequest(accessToken, oAuthReq)
        return oAuthReq.send()
    }

    fun jsonFromGet(url: String, params: Map<String, *>? = null): JSONObject {
        try {
            return checkResult(JSONUtils.jsonFromInputStream(getSignedGetResponse(url, params).stream))
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    fun jsonFromPost(url: String, params: Map<String, *>): JSONObject {
        try {
            return checkResult(JSONUtils.jsonFromInputStream(getSignedPostResponse(url, params).stream))
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    /**
     * Do not involve signed oAuth call, this is used to make public tumblr API requests
     * @param url the public url
     * @param params query parameters
     * @return the json
     */
    fun publicJsonFromGet(url: String, params: Map<String, *>): JSONObject {
        try {
            val sbUrl = StringBuilder(url + "?api_key=" + consumerKey)
            for ((key, value) in params) {
                sbUrl.append("&").append(key).append("=").append(value)
            }
            return checkResult(JSONUtils.jsonFromUrl(sbUrl.toString()))
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    @Throws(JSONException::class)
    private fun checkResult(json: JSONObject): JSONObject {
        if (!json.has("meta")) {
            throw TumblrException("Invalid tumblr response, meta not found")
        }
        val status = json.getJSONObject("meta").getInt("status")
        if (status != 200 && status != 201) {
            var errorMessage = getErrorFromResponse(json)
            if (errorMessage == null) {
                errorMessage = json.getJSONObject("meta").getString("msg")
            }
            throw TumblrException(errorMessage!!)
        }
        return json
    }

    @Throws(JSONException::class)
    private fun getErrorFromResponse(json: JSONObject): String? {
        if (json.has("response")) {
            val array = json.optJSONArray("response")
            // for example when an invalid id is passed the returned response contains an empty array
            if (array != null && array.length() == 0) {
                return null
            }
            val response = json.getJSONObject("response")
            if (response.has("errors")) {
                val errors = response.getJSONArray("errors")
                return (0 until errors.length()).joinToString(",") { errors.getString(it) }
            }
        }
        return null
    }

    private class AccessAsyncTask(private val uri: Uri, private val callback: AuthenticationCallback?, private val prefs: TokenPreference) : AsyncTask<Void, Void, Token>() {
        private var error: Exception? = null

        override fun doInBackground(vararg params: Void): Token? {
            val context = prefs.context
            try {
                val oAuthService = ServiceBuilder()
                        .provider(TumblrApi::class.java)
                        .apiKey(context.getString(R.string.CONSUMER_KEY))
                        .apiSecret(context.getString(R.string.CONSUMER_SECRET))
                        .callback(context.getString(R.string.CALLBACK_URL))
                        .build()
                return oAuthService.getAccessToken(prefs.requestToken,
                        Verifier(uri.getQueryParameter(OAuthConstants.VERIFIER)))
            } catch (e: Exception) {
                error = e
            }

            return null
        }

        override fun onPostExecute(token: Token?) {
            if (token != null && error == null) {
                prefs.storeAccessToken(token)
            }
            if (callback != null) {
                if (token == null) {
                    callback.tumblrAuthenticated(null, null, error)
                } else {
                    callback.tumblrAuthenticated(token.token, token.secret, error)
                }
            }
        }
    }

    private class LoginAsyncTask : AsyncTask<Context, Void, Void>() {
        override fun doInBackground(vararg params: Context): Void? {
            TumblrHttpOAuthConsumer.authorize(params[0])
            return null
        }
    }

    private class TokenPreference(val context: Context) {

        val requestToken: Token
            get() = getToken(context.getSharedPreferences(PREFS_NAME, 0))

        val accessToken: Token
            get() = getToken(PreferenceManager.getDefaultSharedPreferences(context))

        val isAccessTokenValid: Boolean
            get() {
                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                return preferences.contains(PREF_OAUTH_TOKEN) && preferences.contains(PREF_OAUTH_SECRET)
            }

        fun storeRequestToken(requestToken: Token) {
            storeToken(context.getSharedPreferences(PREFS_NAME, 0), requestToken)
        }

        fun storeAccessToken(accessToken: Token) {
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

            private fun getToken(sharedPreferences: SharedPreferences): Token {
                return Token(
                        sharedPreferences.getString(PREF_OAUTH_TOKEN, null),
                        sharedPreferences.getString(PREF_OAUTH_SECRET, null))
            }

            private fun storeToken(sharedPreferences: SharedPreferences, token: Token) {
                sharedPreferences
                        .edit()
                        .putString(PREF_OAUTH_TOKEN, token.token)
                        .putString(PREF_OAUTH_SECRET, token.secret)
                        .apply()
            }
        }
    }

    companion object {

        fun isLogged(context: Context): Boolean {
            return TokenPreference.from(context).isAccessTokenValid
        }

        fun logout(context: Context) {
            TokenPreference.from(context).clearAccessToken()
        }

        private fun authorize(context: Context) {
            // Callback url scheme is defined into manifest
            val oAuthService = ServiceBuilder()
                    .provider(TumblrApi::class.java)
                    .apiKey(context.getString(R.string.CONSUMER_KEY))
                    .apiSecret(context.getString(R.string.CONSUMER_SECRET))
                    .callback(context.getString(R.string.CALLBACK_URL))
                    .build()
            val requestToken = oAuthService.requestToken
            TokenPreference.from(context).storeRequestToken(requestToken)
            val authorizationUrl = oAuthService.getAuthorizationUrl(requestToken)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl))
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            context.startActivity(intent)
        }

        private fun access(context: Context, uri: Uri, callback: AuthenticationCallback) {
            AccessAsyncTask(uri, callback, TokenPreference.from(context)).execute()
        }

        fun loginWithActivity(context: Context) {
            LoginAsyncTask().execute(context)
        }

        /**
         * Return true if the uri scheme can be handled, false otherwise
         * The returned value indicated only the scheme can be handled, the method complete the access asynchronously
         * @param context the context
         * @param uri the uri to check
         * @param callback can be null
         * @return true if uri can be handled, false otherwise
         */
        fun handleOpenURI(context: Context, uri: Uri?, callback: AuthenticationCallback): Boolean {
            val callbackUrl = context.getString(R.string.CALLBACK_URL)

            return if (uri != null && callbackUrl.startsWith(uri.scheme)) {
                TumblrHttpOAuthConsumer.access(
                        context,
                        uri,
                        callback)
                true
            } else false
        }
    }
}
