package com.ternaryop.photoshelf.api.post

import com.ternaryop.photoshelf.api.PhotoShelfApi
import com.ternaryop.utils.json.getMap
import com.ternaryop.utils.json.readJson
import com.ternaryop.utils.json.toList
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder

@Suppress("UNCHECKED_CAST")
data class LatestTimestamp(val importCount: Int, val lastPublishTimestamp: Long, val publishedIdList: List<String>?) {
    constructor(json: JSONObject) :
        this(
            json.getInt("importCount"),
            json.getLong("lastPublishTimestamp"),
            if (json.has("publishedIdList")) json.getJSONArray("publishedIdList").toList() as List<String> else null)
}

data class TagInfo(val tag: String, val postCount: Long)

/**
 * Created by dave on 14/06/2018.
 * Photo Post API Manager
 */
class PostManager(override val accessToken: String) : PhotoShelfApi(accessToken) {
    fun getLastPublishedTimestamp(blogName: String, since: Long): LatestTimestamp {
        val apiUrl = "$API_PREFIX/v1/post/$blogName/latestTimestamp?since=$since"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(apiUrl)
            handleError(conn)
            LatestTimestamp(conn.inputStream.readJson())
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    fun getStats(blogName: String): Map<String, Long> {
        val apiUrl = "$API_PREFIX/v1/post/$blogName/stats"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(apiUrl)
            handleError(conn)
            mapToLongValue(conn.inputStream.readJson(), "stats")
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    fun getCorrectMisspelledName(name: String): String? {
        val apiUrl = "$API_PREFIX/v1/post/tag?misspelled=${URLEncoder.encode(name, "UTF-8")}"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(apiUrl)
            handleError(conn)
            conn.inputStream.readJson().optString("corrected", null)
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getMapLastPublishedTimestampTag(titles: Collection<String>, blogName: String): Map<String, Long> {
        val apiUrl = "$API_PREFIX/v1/post/$blogName/latestTag"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedPostConnection(apiUrl, "text/plain", titles.joinToString("\n"))
            handleError(conn)
            mapToLongValue(conn.inputStream.readJson(), "pairs")
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    fun findTags(pattern: String, blogName: String): List<TagInfo> {
        val apiUrl = "$API_PREFIX/v1/post/$blogName/tags?t=${URLEncoder.encode(pattern, "UTF-8")}"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(apiUrl)
            handleError(conn)
            val arr = conn.inputStream.readJson().getJSONArray("tags")
            val list = mutableListOf<TagInfo>()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                list.add(TagInfo(item.getString("tag"), item.getLong("postCount")))
            }
            list
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    fun editTags(postId: Long, tags: List<String>) {
        val apiUrl = "$API_PREFIX/v1/post/editTags"

        var conn: HttpURLConnection? = null
        try {
            val sb = StringBuilder()
            sb.append("postId=$postId")
            for (t in tags) {
                sb.append("&t[]=").append(URLEncoder.encode(t, "UTF-8"))
            }
            conn = getSignedPostConnection(apiUrl, "application/x-www-form-urlencoded", sb.toString())
            handleError(conn)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    fun deletePost(postId: Long) {
        val apiUrl = "$API_PREFIX/v1/post/$postId"

        var conn: HttpURLConnection? = null
        try {
            conn = getSignedConnection(apiUrl, "DELETE")
            handleError(conn)
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun mapToLongValue(json: JSONObject, name: String) : Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        for (e in json.getMap(name).entries) {
            map[e.key] = (e.value as Number).toLong()
        }
        return map
    }
}
