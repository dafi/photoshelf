package com.ternaryop.photoshelf.birthday

import com.ternaryop.utils.JSONUtils
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Created by dave on 01/04/17.
 * Image Extractor Manager
 */

private const val API_PREFIX = "http://visualdiffer.com/image"

class BirthdayManager(private val accessToken: String) {

    @Throws(Exception::class)
    fun search(name: String): BirthdayInfo {
        val sb = "$API_PREFIX/v1/birthday/search?name=${URLEncoder.encode(name, "UTF-8")}"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(sb)
            handleError(conn)
            val birthdayInfo = toJson(conn.inputStream).getJSONArray("birthdays")
            BirthdayInfo(birthdayInfo.getJSONObject(0))
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    @Throws(IOException::class) private fun getSignedGetConnection(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("PhotoShelf-Subscription-Key", accessToken)
        conn.requestMethod = "GET"

        return conn
    }

    @Throws(Exception::class)
    private fun toJson(stream: InputStream): JSONObject {
        BufferedInputStream(stream).use { bis -> return JSONUtils.jsonFromInputStream(bis) }
    }

    @Throws(Exception::class) private fun handleError(conn: HttpURLConnection) {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            return
        }
        val error = toJson(conn.errorStream)
        throw RuntimeException("Error ${conn.responseCode} : ${error.getString("errorMessage")}")
    }
}
