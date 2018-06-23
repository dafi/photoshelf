package com.ternaryop.photoshelf.api.extractor

import com.ternaryop.photoshelf.api.PhotoShelfApi
import com.ternaryop.utils.json.readJson
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * Created by dave on 01/04/17.
 * Image Extractor Manager
 */

class ImageExtractorManager(override val accessToken: String) : PhotoShelfApi(accessToken) {

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
}
