package com.ternaryop.photoshelf.api.post

import okhttp3.MediaType
import okhttp3.RequestBody

data class LatestTimestampResult(val importCount: Int, val lastPublishTimestamp: Long, val publishedIdList: List<String>?)
data class LatestTagResult(val pairs: Map<String, Long>)
data class StatsResult(val stats: Map<String, Long>)
data class MisspelledResult(val misspelled: String, val corrected: String?)

data class TagInfo(val tag: String, val postCount: Long)
data class TagInfoListResult(val tags: List<TagInfo>)

fun titlesRequestBody(titles: Collection<String>): RequestBody = RequestBody
    .create(MediaType.parse("text/plain"), titles.joinToString("\n"))
