package com.ternaryop.photoshelf.api.post

import okhttp3.MediaType
import okhttp3.RequestBody

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
            val map = HashMap<String, TagInfo>(tagList.size)
            for (s in tagList) {
                val lower = s.toLowerCase()
                var tagInfo = map[lower]
                if (tagInfo == null) {
                    tagInfo = TagInfo(s, 1)
                    map[lower] = tagInfo
                } else {
                    ++tagInfo.postCount
                }
            }
            return map.values.toList()
        }
    }
}

data class TagInfoListResult(val tags: List<TagInfo>)

fun titlesRequestBody(titles: Collection<String>): RequestBody = RequestBody
    .create(MediaType.parse("text/plain"), titles.joinToString("\n"))
