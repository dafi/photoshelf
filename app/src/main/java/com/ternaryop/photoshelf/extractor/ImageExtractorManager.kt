package com.ternaryop.photoshelf.extractor

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

// private val API_PREFIX = "http://10.0.3.2/image";
// private val API_PREFIX = "http://192.168.0.2/image";
private const val API_PREFIX = "http://visualdiffer.com/image"

class ImageExtractorManager(private val accessToken: String) {

    @Throws(Exception::class)
    fun getGallery(url: String): ImageGallery {
        val apiUrl = "$API_PREFIX/v1/extract/gallery?url=${URLEncoder.encode(url, "UTF-8")}"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(apiUrl)
            handleError(conn)
            ImageGallery(toJson(conn.inputStream).getJSONObject("gallery"))
        } finally {
            if (conn != null) try {
                conn.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    @Throws(Exception::class)
    fun getImageUrl(url: String): String {
        val apiUrl = "$API_PREFIX/v1/extract/image?url=${URLEncoder.encode(url, "UTF-8")}"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(apiUrl)
            handleError(conn)
            toJson(conn.inputStream).getString("imageUrl")
        } finally {
            if (conn != null) try {
                conn.disconnect()
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
