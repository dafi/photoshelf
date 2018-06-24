package com.ternaryop.photoshelf.api.birthday

import com.ternaryop.photoshelf.api.PhotoShelfApi
import com.ternaryop.utils.date.fromIsoFormat
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
    fun getByName(name: String, searchIfNew: Boolean): Birthday {
        val sb = "$API_PREFIX/v1/birthday/name/get?name=${URLEncoder.encode(name, "UTF-8")}&searchIfNew=$searchIfNew"

        var conn: HttpURLConnection? = null
        return try {
            conn = getSignedGetConnection(sb)
            handleError(conn)
            val json = conn.inputStream.readJson().getJSONArray("birthdays").getJSONObject(0)
            Birthday(json.getString("name"),
                Calendar.getInstance().fromIsoFormat(json.getString("birthdate")),
                null,
                json.optString("source"))
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

    fun findSameDay(name: String, offset: Int, limit: Int): BirthdayResult {
        return readBirthdayResult("$API_PREFIX/v1/birthday/date/sameday?offset=$offset&limit=$limit&name=$name")
    }

    fun findIgnored(name: String, offset: Int, limit: Int): BirthdayResult {
        return readBirthdayResult("$API_PREFIX/v1/birthday/date/ignored?offset=$offset&limit=$limit&name=$name")
    }

    fun findOrphans(name: String, offset: Int, limit: Int): BirthdayResult {
        return readBirthdayResult("$API_PREFIX/v1/birthday/date/orphans?offset=$offset&limit=$limit&name=$name")
    }

    fun findMissingNames(offset: Int, limit: Int): List<String> {
        val url = "$API_PREFIX/v1/birthday/name/missing?offset=$offset&limit=$limit"
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

            name?.let { sb.append("&name=$it") }
            if (month > 0){
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
    data class BirthdayResult(val total: Long, val birthdates: List<Birthday>?) : Serializable
    data class Birthday(val name: String, val birthdate: Calendar,
        val images: List<ImageSize>?, val source: String? = null) : Serializable
    data class ImageSize(val width: Int, val height: Int, val url: String) : Serializable

    companion object {
        const val MAX_BIRTHDAY_COUNT = 200
    }
}
