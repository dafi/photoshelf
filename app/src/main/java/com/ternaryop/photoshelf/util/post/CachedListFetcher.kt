package com.ternaryop.photoshelf.util.post

import com.ternaryop.photoshelf.lifecycle.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class CachedListFetcher<T> {
    var list: MutableList<T>? = null
        private set

    suspend fun fetch(
        fetcher: suspend () -> List<T>?) : Command<FetchedData<T>> = withContext(Dispatchers.IO) {
        if (isCacheValid()) {
            Command.success(FetchedData(list!!, 0))
        } else {
            try {
                val l = safeList()
                val count = fetcher()?.let { items ->
                    l.addAll(items)
                    items.size
                } ?: 0
                Command.success(FetchedData(l, count))
            } catch (t: Throwable) {
                Command.error<FetchedData<T>>(t)
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