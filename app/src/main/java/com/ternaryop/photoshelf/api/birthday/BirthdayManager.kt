package com.ternaryop.photoshelf.api.birthday

import com.ternaryop.photoshelf.api.PhotoShelfApi
import com.ternaryop.utils.date.fromIsoFormat
import com.ternaryop.utils.date.toIsoFormat
import com.ternaryop.utils.json.readJson
import org.json.JSONArray
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.Calendar

/**
 * Created by dave on 01/04/17.
 * Birthday API Manager
 */

// some images don't have the exact (==) width so we get closest width (<=)
fun BirthdayManager.Birthday.getClosestPhotoByWidth(width: Int):
    BirthdayManager.ImageSize? = images?.firstOrNull { it.width <= width }

class BirthdayManager(override val accessToken: String) : PhotoShelfApi(accessToken) {
    fun getByName(name: String, searchIfNew: Boolean): NameResult {
        val sb = "$API_PREFIX/v1/birthday/name/get?name=${URLEncoder.encode(name, "UTF-8")}&searchIfNew=$searchIfNew"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(sb)
            handleError(conn)
            val json = conn.inputStream.readJson().getJSONObject("response")
            val jsonBirthday = json.getJSONObject("birthday")
            NameResult(Birthday(jsonBirthday.getString("name"),
                Calendar.getInstance().fromIsoFormat(jsonBirthday.getString("birthdate"))),
                json.getBoolean("isNew"))
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    fun findByDate(findParams: FindParams): BirthdayResult {
        return readBirthdayResult("$API_PREFIX/v1/birthday/date/find?${findParams.toQueryString()}")
    }

    fun findByMatchingName(name: String, offset: Int, limit: Int): BirthdayResult {
        return readBirthdayResult("$API_PREFIX/v1/birthday/name/matching?offset=$offset&limit=$limit&name=$name")
    }

    fun findSameDay(findParams: FindParams): BirthdayResult {
        return readBirthdayResult("$API_PREFIX/v1/birthday/date/sameday?${findParams.toQueryString()}")
    }

    fun findIgnored(findParams: FindParams): List<String> {
        return readStringList("$API_PREFIX/v1/birthday/date/ignored?${findParams.toQueryString()}")
    }

    fun findOrphans(findParams: FindParams): BirthdayResult {
        return readBirthdayResult("$API_PREFIX/v1/birthday/date/orphans?${findParams.toQueryString()}")
    }

    fun findMissingNames(findParams: FindParams): List<String> {
        return readStringList("$API_PREFIX/v1/birthday/name/missing?${findParams.toQueryString()}")
    }

    private fun readStringList(url: String): List<String> {
        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(url)
            handleError(conn)
            val data = conn.inputStream.readJson().getJSONObject("response")
            val names = mutableListOf<String>()
            val arr = data.getJSONArray("names")
            for (i in 0 until arr.length()) {
                names.add(arr.getString(i))
            }
            names
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun readBirthdayResult(url: String): BirthdayResult {
        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(url)
            handleError(conn)
            val data = conn.inputStream.readJson().getJSONObject("response")
            BirthdayResult(
                data.optLong("total", 0),
                getBirthdays(data.optJSONArray("birthdays")))
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    private fun getImages(images: JSONArray?): List<ImageSize>? {
        images ?: return null
        val list = mutableListOf<ImageSize>()

        for (i in 0 until images.length()) {
            val image = images.getJSONObject(i)
            list.add(ImageSize(image.getInt("width"), image.getInt("height"), image.getString("url")))
        }
        return list
    }

    private fun getBirthdays(birthdays: JSONArray?): List<Birthday>? {
        birthdays ?: return null
        val list = mutableListOf<Birthday>()
        for (i in 0 until birthdays.length()) {
            val bdate = birthdays.getJSONObject(i)
            list.add(Birthday(bdate.getString("name"),
                Calendar.getInstance().fromIsoFormat(bdate.getString("birthdate")),
                getImages(bdate.optJSONArray("images"))))
        }
        return list
    }

    class FindParams(
        val name: String? = null,
        val month: Int = -1,
        val dayOfMonth: Int = -1,
        var offset: Int = 0,
        val limit: Int = 1000,
        val onlyTotal: Boolean = false,
        val pickImages: Boolean = false,
        val blogName: String? = null) {
        init {
            if (pickImages) {
                requireNotNull(blogName) { "blogName is mandatory with pickImages" }
            }
        }
        fun toQueryString(): String {
            val sb = StringBuilder()

            sb.append("offset=$offset")
                .append("&limit=$limit")
                .append("&onlyTotal=$onlyTotal")

            name?.let { sb.append("&name=${URLEncoder.encode(it, "UTF-8")}") }
            if (month > 0) {
                sb.append("&month=$month")
            }
            if (dayOfMonth > 0) {
                sb.append("&dayOfMonth=$dayOfMonth")
            }
            if (pickImages) {
                sb.append("&pickImages=true")
                    .append("&blogName=$blogName")
            }
            return sb.toString()
        }
    }

    fun deleteByName(name: String) {
        val apiUrl = "$API_PREFIX/v1/birthday/name?name=${URLEncoder.encode(name, "UTF-8")}"

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

    fun updateByName(birthday: Birthday) {
        val apiUrl = "$API_PREFIX/v1/birthday/date/edit?name=${URLEncoder.encode(birthday.name, "UTF-8")}&birthdate=${birthday.birthdate.toIsoFormat()}"

        var conn: HttpURLConnection? = null
        try {
            conn = getSignedConnection(apiUrl, "PUT")
            handleError(conn)
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    fun markAsIgnored(name: String) {
        val apiUrl = "$API_PREFIX/v1/birthday/name/ignore?name=${URLEncoder.encode(name, "UTF-8")}"

        var conn: HttpURLConnection? = null
        try {
            conn = getSignedConnection(apiUrl, "PUT")
            handleError(conn)
        } finally {
            try {
                conn?.disconnect()
            } catch (ignored: Exception) {
            }
        }
    }

    data class NameResult(val birthday: Birthday, val isNew: Boolean)
    data class BirthdayResult(val total: Long, val birthdates: List<Birthday>?) : Serializable
    data class Birthday(val name: String, var birthdate: Calendar,
        val images: List<ImageSize>? = null, val source: String? = null) : Serializable
    data class ImageSize(val width: Int, val height: Int, val url: String) : Serializable

    companion object {
        const val MAX_BIRTHDAY_COUNT = 200
    }
}
