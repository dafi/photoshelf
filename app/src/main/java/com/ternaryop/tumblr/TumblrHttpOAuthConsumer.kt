package com.ternaryop.tumblr

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.httpclient.jdk.JDKHttpClientConfig
import com.github.scribejava.core.model.OAuth1AccessToken
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Response
import com.github.scribejava.core.model.Verb
import com.github.scribejava.core.oauth.OAuth10aService
import com.ternaryop.utils.json.readJson
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TumblrHttpOAuthConsumer(val consumerKey: String,
    apiKey: String, callbackUrl: String, private val accessToken: OAuth1AccessToken) {
    private val oAuthService = createAuthService(consumerKey, apiKey, callbackUrl)

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
        return oAuthService.execute(MultipartConverter(oAuthReq, params).request)
    }

    fun getSignedGetResponse(url: String, params: Map<String, *>?): Response {
        val oAuthReq = OAuthRequest(Verb.GET, url)

        if (params != null) {
            for (key in params.keys) {
                val value = params[key]
                oAuthReq.addQuerystringParameter(key, value.toString())
            }
        }
        oAuthService.signRequest(accessToken, oAuthReq)
        return oAuthService.execute(oAuthReq)
    }

    fun jsonFromGet(url: String, params: Map<String, *>? = null): JSONObject {
        try {
            return checkResult(getSignedGetResponse(url, params).stream.readJson())
        } catch (e: Exception) {
            throw TumblrException(e)
        }
    }

    fun jsonFromPost(url: String, params: Map<String, *>): JSONObject {
        try {
            return checkResult(getSignedPostResponse(url, params).stream.readJson())
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
            val sbUrl = StringBuilder("$url?api_key=$consumerKey")
            for ((key, value) in params) {
                sbUrl.append("&").append(key).append("=").append(value)
            }
            return checkResult(URL(sbUrl.toString()).readJson())
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

        if (status != HttpURLConnection.HTTP_OK && status != HttpURLConnection.HTTP_CREATED) {
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

    companion object {
        private const val CONNECTION_TIMEOUT_MILLIS = 3000
        fun createAuthService(consumerKey: String, apiSecret: String, callbackUrl: String): OAuth10aService {
            return ServiceBuilder(consumerKey)
                .apiSecret(apiSecret)
                .callback(callbackUrl)
                .httpClientConfig(
                    JDKHttpClientConfig.defaultConfig().apply { connectTimeout = CONNECTION_TIMEOUT_MILLIS })
                .build(TumblrApiFix.instance)
        }
    }
}
