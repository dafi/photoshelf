package com.ternaryop.photoshelf.fragment.feedly

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.ternaryop.feedly.AccessToken
import com.ternaryop.feedly.Category
import com.ternaryop.feedly.FeedlyClient
import com.ternaryop.feedly.SimpleFeedlyContent
import com.ternaryop.feedly.StreamContent
import com.ternaryop.feedly.StreamContentFindParam
import com.ternaryop.photoshelf.BuildConfig
import com.ternaryop.photoshelf.PhotoShelfApplication
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.adapter.feedly.FeedlyContentDelegate
import com.ternaryop.photoshelf.adapter.feedly.titles
import com.ternaryop.photoshelf.adapter.feedly.toContentDelegate
import com.ternaryop.photoshelf.adapter.feedly.updateLastPublishTimestamp
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.titlesRequestBody
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStreamReader

private const val ONE_HOUR_MILLIS = 60 * 60 * 1000

class FeedlyViewModel(application: Application) : PhotoShelfViewModel<FeedlyModelResult>(application) {
    private val preferences = FeedlyPrefs(application)
    private val feedlyClient = FeedlyClient(
    preferences.accessToken ?: application.getString(R.string.FEEDLY_ACCESS_TOKEN),
        application.getString(R.string.FEEDLY_USER_ID),
        application.getString(R.string.FEEDLY_REFRESH_TOKEN))

    fun refreshContent(blogName: String, idListToDelete: List<String>?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = getFeedlyContentDelegate(blogName, idListToDelete)
                postResult(FeedlyModelResult.Content(Command.success(list)))
            } catch (t: Throwable) {
                postResult(FeedlyModelResult.Content(Command.error(t)))
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                postResult(FeedlyModelResult.Categories(Command.success(feedlyClient.getCategories())))
            } catch (t: Throwable) {
                postResult(FeedlyModelResult.Categories(Command.error(t)))
            }
        }
    }

    fun refreshToken() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = feedlyClient.refreshAccessToken(
                    getApplication<PhotoShelfApplication>().getString(R.string.FEEDLY_CLIENT_ID),
                    getApplication<PhotoShelfApplication>().getString(R.string.FEEDLY_CLIENT_SECRET))

                preferences.accessToken = token.accessToken
                feedlyClient.accessToken = token.accessToken
                postResult(FeedlyModelResult.AccessTokenRefresh(Command.success(token)))
            } catch (t: Throwable) {
                postResult(FeedlyModelResult.AccessTokenRefresh(Command.error(t)))
            }
        }
    }

    fun markSaved(data: MarkSavedData) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                feedlyClient.markSaved(data.idList, data.checked)
                postResult(FeedlyModelResult.MarkSaved(Command.success(data)))
            } catch (t: Throwable) {
                postResult(FeedlyModelResult.MarkSaved(Command.error(t)))
            }
        }
    }

    private suspend fun getFeedlyContentDelegate(blogName: String, idListToDelete: List<String>?): List<FeedlyContentDelegate> {
        val list = filterCategories(readStreamContent(idListToDelete)).toContentDelegate()
        val map = ApiManager.postService().getMapLastPublishedTimestampTag(blogName, titlesRequestBody(list.titles()))
        list.updateLastPublishTimestamp(map.response.pairs)

        return list
    }

    private fun filterCategories(streamContent: StreamContent): List<SimpleFeedlyContent> {
        val selectedCategories = preferences.selectedCategoriesId

        if (selectedCategories.isEmpty()) {
            return streamContent.items
        }
        return streamContent.items.filter { sc ->
            sc.categories?.any { cat -> selectedCategories.any { cat.id == it } } ?: true
        }
    }

    private suspend fun readStreamContent(idListToDelete: List<String>?): StreamContent {
        return if (BuildConfig.DEBUG) {
            fakeCall()
        } else {
            idListToDelete?.also { feedlyClient.markSaved(it, false) }
            getNewerSavedContent()
        }
    }

    private suspend fun getNewerSavedContent(): StreamContent {
        val ms = System.currentTimeMillis() - preferences.newerThanHours * ONE_HOUR_MILLIS
        val params = StreamContentFindParam(preferences.maxFetchItemCount, ms)
        return feedlyClient.getStreamContents(feedlyClient.globalSavedTag, params.toQueryMap())
    }

    private fun fakeCall(): StreamContent {
        return getApplication<PhotoShelfApplication>().assets.open("sample/feedly.json").use { stream ->
            GsonBuilder().create().fromJson(InputStreamReader(stream), StreamContent::class.java)
        }
    }

}

data class MarkSavedData(val idList: List<String>, val checked: Boolean, val positionList: List<Int>)

sealed class FeedlyModelResult {
    data class AccessTokenRefresh(val command: Command<AccessToken>) : FeedlyModelResult()
    data class Content(val command: Command<List<FeedlyContentDelegate>>) : FeedlyModelResult()
    data class MarkSaved(val command: Command<MarkSavedData>) : FeedlyModelResult()
    data class Categories(val command: Command<List<Category>>) : FeedlyModelResult()
}
