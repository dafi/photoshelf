package com.ternaryop.feedly

import com.ternaryop.utils.JSONUtils
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Created by dave on 24/02/17.
 * Feedly Manager
 */

private const val API_PREFIX = "https://cloud.feedly.com"

class FeedlyManager(var accessToken: String, userId: String, private val refreshToken: String) {

    val globalSavedTag = "user/$userId/tag/global.saved"

    @Throws(Exception::class)
    fun getStreamContents(streamId: String, count: Int, newerThan: Long, continuation: String?): List<FeedlyContent> {
        var apiUrl = "$API_PREFIX/v3/streams/contents?streamId=$streamId"

        if (count > 0) {
            apiUrl += "&count=$count"
        }
        if (newerThan > 0) {
            apiUrl += "&newerThan=$newerThan"
        }
        if (continuation != null) {
            apiUrl += "&continuation=$continuation"
        }
        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(apiUrl)
            handleError(conn)
            FeedlyRateLimit.update(conn)

            val items = toJson(conn.inputStream).getJSONArray("items")
            Array(items.length(), { SimpleFeedlyContent(items.getJSONObject(it)) as FeedlyContent }).toList()
        } finally {
            if (conn != null) try {
                conn.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    @Throws(Exception::class)
    fun markSaved(ids: List<String>, saved: Boolean) {
        if (ids.isEmpty()) {
            return
        }
        val map = mapOf(
                "type" to "entries",
                "action" to if (saved) "markAsSaved" else "markAsUnsaved",
                "entryIds" to ids
        )
        val data = JSONUtils.toJSON(map).toString()

        var conn: HttpURLConnection? = null
        try {
            conn = getSignedPostConnection("$API_PREFIX/v3/markers", "application/json", data)
            FeedlyRateLimit.update(conn)
            handleError(conn)
        } finally {
            if (conn != null) try {
                conn.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    @Throws(Exception::class)
    fun refreshAccessToken(clientId: String, clientSecret: String): String {
        var conn: HttpURLConnection? = null
        try {
            val data = ("refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}"
                    + "&client_id=${URLEncoder.encode(clientId, "UTF-8")}"
                    + "&client_secret=${URLEncoder.encode(clientSecret, "UTF-8")}"
                    + "&grant_type=${URLEncoder.encode("refresh_token", "UTF-8")}")

            conn = getSignedPostConnection("$API_PREFIX/v3/auth/token", "application/x-www-form-urlencoded", data)
            FeedlyRateLimit.update(conn)

            handleError(conn)
            return toJson(conn.inputStream).getString("access_token")
        } finally {
            if (conn != null) try {
                conn.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    @Throws(IOException::class)
    private fun getSignedPostConnection(url: String, contentType: String, data: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "OAuth $accessToken")
        conn.setRequestProperty("Content-Type", contentType)
        conn.doInput = true
        conn.doOutput = true
        conn.requestMethod = "POST"
        conn.useCaches = false
        conn.instanceFollowRedirects = false

        conn.outputStream.use { os -> os.write(data.toByteArray(StandardCharsets.UTF_8)) }

        return conn
    }

    @Throws(IOException::class)
    private fun getSignedGetConnection(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("Authorization", "OAuth $accessToken")
        conn.requestMethod = "GET"

        return conn
    }

    @Throws(Exception::class)
    private fun toJson(stream: InputStream): JSONObject {
        BufferedInputStream(stream).use { bis -> return JSONUtils.jsonFromInputStream(bis) }
    }

    @Throws(Exception::class)
    private fun handleError(conn: HttpURLConnection) {
        val responseCode = conn.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return
        }
        val error = toJson(conn.errorStream)
        val errorMessage = error.getString("errorMessage")
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            if (errorMessage != null && errorMessage.startsWith("token expired")) {
                throw TokenExpiredException(errorMessage)
            }
        }
        throw RuntimeException("Error $responseCode: $errorMessage")
    }
}
