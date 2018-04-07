package com.ternaryop.photoshelf.extractor

import com.ternaryop.utils.json.readJson
import java.io.IOException
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
            ImageGallery(conn.inputStream.readJson().getJSONObject("gallery"))
        } finally {
            try {
                conn?.disconnect()
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
            conn.inputStream.readJson().getString("imageUrl")
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

    @Throws(Exception::class) private fun handleError(conn: HttpURLConnection) {
        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
            return
        }
        val error = conn.errorStream.readJson()
        throw RuntimeException("Error ${conn.responseCode} : ${error.getString("errorMessage")}")
    }
}
