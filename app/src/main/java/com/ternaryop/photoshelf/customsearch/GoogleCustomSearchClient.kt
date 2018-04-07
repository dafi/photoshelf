package com.ternaryop.photoshelf.customsearch

import com.ternaryop.utils.json.readJson
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Created by dave on 30/04/17.
 * Mini Client interface for Google Custom Search (CSE)
 * https://cse.google.com/cse/all
 * https://developers.google.com/custom-search/json-api/v1/using_rest
 */

private const val API_PREFIX = "https://www.googleapis.com/customsearch"

class GoogleCustomSearchClient(private val apiKey: String, private val cx: String) {

    @Throws(Exception::class)
    fun search(q: String, fields: Array<String>? = null): CustomSearchResult {
        return CustomSearchResult(getJSON(q, fields))
    }

    @Throws(Exception::class)
    fun getCorrectedQuery(q: String): String? {
        return CustomSearchResult.getCorrectedQuery(getJSON(q, arrayOf("spelling")))
    }

    @Throws(Exception::class)
    private fun getJSON(q: String, fields: Array<String>?): JSONObject {
        var apiUrl = "$API_PREFIX/v1?key=$apiKey&cx=$cx&q=${URLEncoder.encode(q, "UTF-8")}"

        if (fields != null) {
            apiUrl += "&fields=${fields.joinToString(",")}"
        }
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.inputStream.readJson()
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }
}