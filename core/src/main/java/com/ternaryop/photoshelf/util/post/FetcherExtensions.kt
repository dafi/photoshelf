package com.ternaryop.photoshelf.util.post

fun <T> PageFetcher<T>.removeItems(items: List<T>) {
    list?.also { list ->
        items.forEach { list.remove(it) }
    }
}

fun <T> PageFetcher<T>.removeItem(item: T) {
    list?.remove(item)
}

fun <T> CachedListFetcher<T>.moveToBottom(index: Int) {
    list?.also { list ->
        if (list.isEmpty() || index == (list.size - 1)) {
            return
        }
        list.add(list.removeAt(index))
    }
}
