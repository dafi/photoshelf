package com.ternaryop.photoshelf.util.post

import com.ternaryop.photoshelf.lifecycle.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FetchedData<T>(val list: List<T>, val lastFetchCount: Int)

class PageFetcher<T>(val limitCount: Int) {
    var list: MutableList<T>? = null
        private set
    private val pagingScroll = PagingScroll(limitCount)

    val pagingInfo: PagingInfo
        get() = pagingScroll

    suspend fun fetch(
        fetchCache: Boolean,
        fetcher: suspend (pagingInfo: PagingInfo) -> List<T>?) : Command<FetchedData<T>> = withContext(Dispatchers.IO) {
        if (fetchCache && list != null) {
            Command.success(FetchedData(list!!, 0))
        } else {
            try {
                val l = safeList()
                val count = fetcher(pagingInfo)?.let { items ->
                    pagingScroll.incrementReadItemCount(items.size)
                    l.addAll(items)
                    items.size
                } ?: 0
                Command.success(FetchedData(l, count))
            } catch (t: Throwable) {
                Command.error<FetchedData<T>>(t)
            } finally {
                pagingScroll.isScrolling = false
            }
        }
    }

    private fun safeList(): MutableList<T> {
        val l = list ?: mutableListOf()
        if (list == null) {
            list = l
        }
        return l
    }

    fun clear() {
        list = null
        pagingScroll.reset()
    }

    fun changedScrollPosition(firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int): Boolean {
        return pagingScroll.changedScrollPosition(firstVisibleItem, visibleItemCount, totalItemCount)
    }
}

