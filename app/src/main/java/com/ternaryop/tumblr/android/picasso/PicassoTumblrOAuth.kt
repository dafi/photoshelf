package com.ternaryop.tumblr.android.picasso

import android.annotation.SuppressLint
import android.content.Context
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.android.TumblrManager
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.ByteArrayOutputStream

class OAuthDownloader(val tumblr: Tumblr) : Downloader {
    override fun shutdown() {
    }

    override fun load(request: Request): Response {
        val url = request.url.toString()
        val buffer = ByteArrayOutputStream()
        val oauthResponse = tumblr.consumer.getSignedGetResponse(url, null)
        oauthResponse.stream.use { stream ->
            stream.copyTo(buffer)

            return Response.Builder()
                .code(oauthResponse.code)
                .protocol(Protocol.HTTP_1_1)
                .request(Request.Builder().url(url).build())
                .message(oauthResponse.message)
                .body(buffer.toByteArray().toResponseBody())
                .build()
        }
    }
}

object PicassoTumblrOAuth {
    @SuppressLint("StaticFieldLeak")
    private var instance: Picasso? = null

    fun get(context: Context): Picasso {
        if (instance == null) {
            instance = Picasso.Builder(context).downloader(OAuthDownloader(TumblrManager.getInstance(context))).build()
        }
        return instance!!
    }
}