package com.ternaryop.photoshelf.feedly.fragment

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ternaryop.feedly.AccessToken
import com.ternaryop.feedly.Category
import com.ternaryop.feedly.FeedlyClient
import com.ternaryop.feedly.SimpleFeedlyContent
import com.ternaryop.feedly.StreamContent
import com.ternaryop.feedly.StreamContentFindParam
import com.ternaryop.feedly.markRead
import com.ternaryop.feedly.markSaved
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.api.post.LastPublishedTitleHolder
import com.ternaryop.photoshelf.feedly.adapter.FeedlyContentDelegate
import com.ternaryop.photoshelf.feedly.adapter.titles
import com.ternaryop.photoshelf.feedly.adapter.toContentDelegate
import com.ternaryop.photoshelf.feedly.adapter.updateLastPublishTimestamp
import com.ternaryop.photoshelf.feedly.prefs.FeedlyPrefs
import com.ternaryop.photoshelf.feedly.reader.StreamContentReader
import com.ternaryop.photoshelf.lifecycle.Command
import com.ternaryop.photoshelf.lifecycle.PhotoShelfViewModel
import com.ternaryop.photoshelf.util.post.CachedListFetcher
import com.ternaryop.photoshelf.util.post.FetchedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedlyViewModel @Inject constructor(
    application: Application,
    private val contentReader: StreamContentReader
) : PhotoShelfViewModel<FeedlyModelResult>() {
    private val preferences = FeedlyPrefs(application)
    private val feedlyClient = FeedlyClient(preferences.accessToken ?: "")
    val contentList = CachedListFetcher<FeedlyContentDelegate>()

    fun readLater(blogName: String, idListToDelete: List<String>?) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = contentList.fetch(
                { idListToDelete?.also { feedlyClient.markSaved(it, false) } },
                { getFeedlyContentDelegate(blogName) }
            )
            postResult(FeedlyModelResult.Content(command, FeedlyContentType.ReadLater))
        }
    }

    fun unread(blogName: String, idListToDelete: List<String>?) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = contentList.fetch(
                { idListToDelete?.also { feedlyClient.markRead(it, true) } },
                { unreadContentDelegate(blogName) }
            )
            postResult(FeedlyModelResult.Content(command, FeedlyContentType.Unread))
        }
    }

    fun loadCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute { feedlyClient.getCategories() }
            postResult(FeedlyModelResult.Categories(command))
        }
    }

    fun refreshToken() {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                feedlyClient.refreshAccessToken().also { token ->
                    preferences.accessToken = token.accessToken
                    feedlyClient.accessToken = token.accessToken
                }
            }
            postResult(FeedlyModelResult.AccessTokenRefresh(command))
        }
    }

    fun markSaved(data: MarkSavedData) {
        viewModelScope.launch(Dispatchers.IO) {
            val command = Command.execute {
                feedlyClient.markSaved(data.idList, data.checked)
                data
            }
            postResult(FeedlyModelResult.MarkSaved(command))
        }
    }

    private suspend fun getFeedlyContentDelegate(blogName: String): List<FeedlyContentDelegate> {
        val list = filterCategories(contentReader.read(feedlyClient)).toContentDelegate()
        val map = ApiManager.postService().getLastPublishedTag(blogName, LastPublishedTitleHolder(list.titles()))
        list.updateLastPublishTimestamp(map.response.tags)

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

    private suspend fun unreadContentDelegate(blogName: String): List<FeedlyContentDelegate> {
        val params = StreamContentFindParam(preferences.maxFetchItemCount, unreadOnly = true).toQueryMap()
        val list = mutableListOf<FeedlyContentDelegate>()
        preferences.selectedCategoriesId.forEach { category ->
            list += feedlyClient.getStreamContents(category, params).items.toContentDelegate()
        }
        val map = ApiManager.postService().getLastPublishedTag(blogName, LastPublishedTitleHolder(list.titles()))
        list.updateLastPublishTimestamp(map.response.tags)

        return list
    }
}

data class MarkSavedData(val idList: List<String>, val checked: Boolean, val positionList: List<Int>)

sealed class FeedlyModelResult {
    data class AccessTokenRefresh(val command: Command<AccessToken>) : FeedlyModelResult()
    data class Content(
        val command: Command<FetchedData<FeedlyContentDelegate>>,
        val type: FeedlyContentType
    ) : FeedlyModelResult()
    data class MarkSaved(val command: Command<MarkSavedData>) : FeedlyModelResult()
    data class Categories(val command: Command<List<Category>>) : FeedlyModelResult()
}
