package com.ternaryop.photoshelf.feedly.reader

import android.content.res.AssetManager
import com.google.gson.GsonBuilder
import com.ternaryop.feedly.FeedlyClient
import com.ternaryop.feedly.StreamContent
import com.ternaryop.feedly.StreamContentFindParam
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

private const val ONE_HOUR_MILLIS = 60 * 60 * 1000

interface StreamContentReader {
    suspend fun read(feedlyClient: FeedlyClient): StreamContent
}

class AssetManagerStreamContentReader(
    private val assets: AssetManager,
    private val fileName: String
) : StreamContentReader {
    override suspend fun read(feedlyClient: FeedlyClient): StreamContent = withContext(Dispatchers.IO) {
        assets.open(fileName).use { stream ->
            GsonBuilder().create().fromJson(InputStreamReader(stream), StreamContent::class.java)
        }
    }
}

class ApiStreamContentReader(
    private val feedlyPrefs: FeedlyPrefs
) : StreamContentReader {
    override suspend fun read(feedlyClient: FeedlyClient): StreamContent {
        val ms = System.currentTimeMillis() - feedlyPrefs.newerThanHours * ONE_HOUR_MILLIS
        val params = StreamContentFindParam(feedlyPrefs.maxFetchItemCount, ms)
        return feedlyClient.getStreamContents(feedlyClient.globalSavedTag, params.toQueryMap())
    }
}
