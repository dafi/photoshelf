package com.ternaryop.tumblr

import com.github.scribejava.core.builder.api.DefaultApi10a

/**
 * Created by dave on 19/02/18.
 * Tumblr suddenly switched to HTTPS for token resource urls so this class overrides the original one
 */
class TumblrApiFix : DefaultApi10a() {
    override fun getAccessTokenEndpoint(): String = ACCESS_TOKEN_RESOURCE
    override fun getRequestTokenEndpoint(): String = REQUEST_TOKEN_RESOURCE
    override fun getAuthorizationBaseUrl(): String = AUTHORIZE_URL

    companion object {
        val instance = TumblrApiFix()
        private const val AUTHORIZE_URL = "https://www.tumblr.com/oauth/authorize"
        private const val REQUEST_TOKEN_RESOURCE = "https://www.tumblr.com/oauth/request_token"
        private const val ACCESS_TOKEN_RESOURCE = "https://www.tumblr.com/oauth/access_token"
    }
}