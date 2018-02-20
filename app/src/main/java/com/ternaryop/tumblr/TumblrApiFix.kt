package com.ternaryop.tumblr

import org.scribe.builder.api.TumblrApi
import org.scribe.model.Token

/**
 * Created by dave on 19/02/18.
 * Tumblr suddenly switched to HTTPS for token resource urls so this class overrides the original one
 */
class TumblrApiFix : TumblrApi() {
    override fun getAccessTokenEndpoint(): String {
        return ACCESS_TOKEN_RESOURCE
    }

    override fun getRequestTokenEndpoint(): String {
        return REQUEST_TOKEN_RESOURCE
    }

    override fun getAuthorizationUrl(requestToken: Token): String {
        return String.format(AUTHORIZE_URL, requestToken.token)
    }

    companion object {
        private const val AUTHORIZE_URL = "https://www.tumblr.com/oauth/authorize?oauth_token=%s"
        private const val REQUEST_TOKEN_RESOURCE = "https://www.tumblr.com/oauth/request_token"
        private const val ACCESS_TOKEN_RESOURCE = "https://www.tumblr.com/oauth/access_token"
    }
}