package com.ternaryop.photoshelf.util.post

/**
 * Hold position information about items read while scrolling the view
 */
interface PagingInfo {
    val offset: Int
    val hasMoreItems: Boolean
    val totalItems: Int
}

class PagingScroll(private val limitCount: Int) : PagingInfo {
    private var _offset = 0
    private var _hasMoreItems = true
    private var _totalItems = 0
    var isScrolling = false

    override val offset: Int
        get() = _offset
    override val hasMoreItems: Boolean
        get() = _hasMoreItems
    override val totalItems: Int
        get() = _totalItems

    /**
     * @return true if the scroll position requires new fetch, false otherwise
     */
    fun changedScrollPosition(firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int): Boolean {
        val loadMore = totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount

        if (loadMore && hasMoreItems && !isScrolling) {
            _offset += limitCount
            if (!isScrolling) {
                isScrolling = true
                return true
            }
        }
        return false
    }

    fun reset() {
        _offset = 0
        _totalItems = 0
        _hasMoreItems = true
        isScrolling = false
    }

    fun incrementReadItemCount(count: Int) {
        if (_hasMoreItems) {
            _totalItems += count
            _hasMoreItems = count == limitCount
        }
    }
}
