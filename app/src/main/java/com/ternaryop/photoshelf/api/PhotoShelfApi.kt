package com.ternaryop.photoshelf.api

import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.utils.json.readJson
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

abstract class PhotoShelfApi(protected open val accessToken: String) {
    protected fun getSignedGetConnection(url: String): HttpURLConnection {
        return getSignedConnection(url, "GET")
    }

    protected fun getSignedConnection(url: String, requestMethod: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("PhotoShelf-Subscription-Key", accessToken)
        conn.requestMethod = requestMethod

        return conn
    }

    protected fun getSignedPostConnection(url: String, contentType: String, data: String): HttpURLConnection {
        val conn = getSignedConnection(url, "POST")
        conn.setRequestProperty("Content-Type", contentType)
        conn.doInput = true
        conn.doOutput = true
        conn.useCaches = false
        conn.instanceFollowRedirects = false

        conn.outputStream.use { os -> os.write(data.toByteArray(StandardCharsets.UTF_8)) }

        return conn
    }

    protected fun handleError(conn: HttpURLConnection) {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            return
        }
        val error = conn.errorStream.readJson()
        throw RuntimeException("Error ${conn.responseCode} : ${error.getString("errorMessage")}")
    }

    companion object {
        const val API_PREFIX = BuildConfig.PHOTOSHELF_API_PREFIX
    }
}
