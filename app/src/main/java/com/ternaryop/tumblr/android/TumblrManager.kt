package com.ternaryop.tumblr.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.scribejava.core.model.OAuthConstants
import com.ternaryop.photoshelf.R
import com.ternaryop.tumblr.AuthenticationCallback
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrException
import com.ternaryop.tumblr.TumblrHttpOAuthConsumer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

object TumblrManager {
    private var instance: Tumblr? = null

    fun getInstance(context: Context): Tumblr {
        if (instance == null) {
            instance = Tumblr(TumblrHttpOAuthConsumer(context.getString(R.string.CONSUMER_KEY),
                context.getString(R.string.CONSUMER_SECRET),
                context.getString(R.string.CALLBACK_URL),
                TokenPreference.from(context).accessToken))
        }
        return instance!!
    }

    fun isLogged(context: Context): Boolean = TokenPreference.from(context).isAccessTokenValid

    fun login(context: Context): Completable {
        return Completable.fromAction { authorize(context) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun logout(context: Context) = TokenPreference.from(context).clearAccessToken()

    private fun authorize(context: Context) {
        // Callback url scheme is defined into manifest
        val oAuthService = TumblrHttpOAuthConsumer.createAuthService(context.getString(R.string.CONSUMER_KEY),
            context.getString(R.string.CONSUMER_SECRET),
            context.getString(R.string.CALLBACK_URL))
        val requestToken = oAuthService.requestToken
        TokenPreference.from(context).storeRequestToken(requestToken)
        val authorizationUrl = oAuthService.getAuthorizationUrl(requestToken)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl))
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(intent)
    }

    private fun access(context: Context, uri: Uri, callback: AuthenticationCallback) {
        val prefs = TokenPreference.from(context)
        Single
            .fromCallable {
                TumblrHttpOAuthConsumer.createAuthService(context.getString(R.string.CONSUMER_KEY),
                    context.getString(R.string.CONSUMER_SECRET),
                    context.getString(R.string.CALLBACK_URL))
                    .getAccessToken(prefs.requestToken, uri.getQueryParameter(OAuthConstants.VERIFIER))
                    ?: throw TumblrException("Invalid token")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ token ->
                prefs.storeAccessToken(token)
                callback.tumblrAuthenticated(token.token, token.tokenSecret)
            }, { callback.tumblrAuthenticationError(it) })
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

        return if (uri != null && callbackUrl.startsWith(uri.scheme!!)) {
            access(context, uri, callback)
            true
        } else false
    }
}