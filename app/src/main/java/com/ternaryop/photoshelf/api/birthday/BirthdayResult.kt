package com.ternaryop.photoshelf.api.birthday

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.Calendar

data class NameResult(
    @SerializedName("birthday") val birthday: Birthday,
    @SerializedName("isNew") val isNew: Boolean)

data class ListResult(@SerializedName("names") val names: List<String>)

data class BirthdayResult(
    @SerializedName("total") val total: Long,
    @SerializedName("birthdays") val birthdays: List<Birthday>?) : Serializable

data class Birthday(
    @SerializedName("name") val name: String,
    @SerializedName("birthdate") var birthdate: Calendar,
    @SerializedName("images") val images: List<ImageSize>? = null,
    @SerializedName("source") val source: String? = null) : Serializable

data class ImageSize(
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int,
    @SerializedName("url") val url: String) : Serializable

// some images don't have the exact (==) width so we get closest width (<=)
fun Birthday.getClosestPhotoByWidth(width: Int):
    ImageSize? = images?.firstOrNull { it.width <= width }

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

    fun toQueryMap(): Map<String, String> {
        val map = mutableMapOf(
            "offset" to offset.toString(),
            "limit" to limit.toString(),
            "onlyTotal" to onlyTotal.toString()
        )

        name?.let { map.put("name", it) }
        if (month > 0) {
            map["month"] = month.toString()
        }
        if (dayOfMonth > 0) {
            map["dayOfMonth"] = dayOfMonth.toString()
        }
        if (pickImages) {
            map["pickImages"] = "true"
            map["blogName"] = blogName!!
        }
        return map
    }
}
