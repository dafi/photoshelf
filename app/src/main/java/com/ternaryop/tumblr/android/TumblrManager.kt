package com.ternaryop.tumblr.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.scribejava.core.model.OAuthConstants
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrException
import com.ternaryop.tumblr.TumblrHttpOAuthConsumer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

object TumblrManager {
    private var instance: Tumblr? = null
    private var consumerKey = ""
    private var consumerSecret = ""
    private var callbackUrl = ""

    fun setup(consumerKey: String,
        consumerSecret: String,
        callbackUrl: String) {
        this.consumerKey = consumerKey
        this.consumerSecret = consumerSecret
        this.callbackUrl = callbackUrl
    }

    fun getInstance(context: Context): Tumblr {
        if (instance == null) {
            instance = Tumblr(TumblrHttpOAuthConsumer(consumerKey,
                consumerSecret,
                callbackUrl,
                TokenPreference.from(context).accessToken))
        }
        return instance!!
    }

    fun isLogged(context: Context): Boolean = TokenPreference.from(context).isAccessTokenValid

    fun login(context: Context): Completable = Completable.fromAction { authorize(context) }

    fun logout(context: Context) = TokenPreference.from(context).clearAccessToken()

    private fun authorize(context: Context) {
        // Callback url scheme is defined into manifest
        val oAuthService = TumblrHttpOAuthConsumer.createAuthService(consumerKey,
            consumerSecret,
            callbackUrl)
        val requestToken = oAuthService.requestToken
        TokenPreference.from(context).storeRequestToken(requestToken)
        val authorizationUrl = oAuthService.getAuthorizationUrl(requestToken)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl))
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        context.startActivity(intent)
    }

    private fun access(context: Context, uri: Uri): Single<Boolean> {
        val prefs = TokenPreference.from(context)
        return Single
            .fromCallable {
                TumblrHttpOAuthConsumer.createAuthService(consumerKey,
                    consumerSecret,
                    callbackUrl)
                    .getAccessToken(prefs.requestToken, uri.getQueryParameter(OAuthConstants.VERIFIER))
                    ?: throw TumblrException("Invalid token")
            }
            .doOnSuccess { token -> prefs.storeAccessToken(token)}
            .flatMap { Single.just(true) }
    }

    /**
     * Return true if the uri scheme can be handled, false otherwise
     * The returned value indicated only the scheme can be handled, the method complete the access asynchronously
     * @param context the context
     * @param uri the uri to check
     * @return true if uri can be handled, false otherwise
     */
    fun handleOpenURI(context: Context, uri: Uri?): Observable<Boolean> {
        return if (uri != null && callbackUrl.startsWith(uri.scheme!!)) access(context, uri).toObservable()
        else Observable.just(false)
    }
}