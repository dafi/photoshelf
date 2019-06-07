package com.ternaryop.photoshelf.api.post

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

data class LatestTimestampResult(val importCount: Int,
    val lastPublishTimestamp: Long,
    val publishedIdList: List<String>?)
data class LatestTagResult(val pairs: Map<String, Long>)
data class StatsResult(val stats: Map<String, Long>)
data class MisspelledResult(val misspelled: String, val corrected: String?)

data class TagInfo(val tag: String, var postCount: Long) {
    fun compareTagTo(other: TagInfo): Int = tag.compareTo(other.tag, ignoreCase = true)

    companion object {
        fun fromStrings(tagList: List<String>): List<TagInfo> {
            return tagList
                .groupingBy { it.toLowerCase() }
                .fold(
                    { key, _ -> TagInfo(key, 0)},
                    { _, tagInfo, _ -> tagInfo.also { ++it.postCount}})
                .values.toList()
        }
    }
}

data class TagInfoListResult(val tags: List<TagInfo>)

fun titlesRequestBody(titles: Collection<String>): RequestBody =
    titles.joinToString("\n").toRequestBody("text/plain".toMediaTypeOrNull())
