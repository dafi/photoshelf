package com.ternaryop.photoshelf.util.post

import com.ternaryop.photoshelf.lifecycle.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class CachedListFetcher<T> {
    var list: MutableList<T>? = null
        private set

    /**
     * Fetch from cache if any, otherwise call [fetcher]
     * [init] is always called ignoring the cache state, if it succeeds then fetch from cache or call [fetcher]
     */
    suspend fun fetch(
        init: (suspend () -> Unit)? = null,
        fetcher: suspend () -> List<T>?
    ): Command<FetchedData<T>> = withContext(Dispatchers.IO) {
        if (init == null) {
            fetchCached(fetcher)
        } else {
            try {
                init()
                fetchCached(fetcher)
            } catch (expected: Throwable) {
                Command.error<FetchedData<T>>(expected)
            }
        }
    }

    private suspend fun fetchCached(fetcher: suspend () -> List<T>?): Command<FetchedData<T>> {
        return if (isCacheValid()) {
            Command.success(FetchedData(checkNotNull(list), 0))
        } else {
            try {
                val l = safeList()
                val count = fetcher()?.let { items ->
                    l.addAll(items)
                    items.size
                } ?: 0
                Command.success(FetchedData(l, count))
            } catch (expected: Throwable) {
                Command.error<FetchedData<T>>(expected)
            }
        }
    }

    open fun isCacheValid() = list != null

    private fun safeList(): MutableList<T> {
        val l = list ?: mutableListOf()
        if (list == null) {
            list = l
        }
        return l
    }

    fun clear() {
        list = null
    }
}
