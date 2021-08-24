package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import com.ternaryop.photoshelf.tumblr.ui.core.adapter.LastPublishedTimestampComparator
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost
import com.ternaryop.photoshelf.util.sort.AbsSortable

typealias PhotoShelfPostSortable = AbsSortable<PhotoShelfPost>

/**
 * Created by dave on 10/03/18.
 * Hold sort types supported by Photo Viewer
 */
class PhotoSortSwitcher {
    private val tagNameSortable: PhotoShelfPostSortable by lazy { TagNameSortable(true) }
    private val uploadTimeSortable: PhotoShelfPostSortable by lazy { UploadTimeSortable(true) }
    private var lastPublishedTagSortable: PhotoShelfPostSortable = LastPublishedTagSortable(true)

    var sortable = lastPublishedTagSortable
        private set

    fun sort(items: MutableList<PhotoShelfPost>) {
        sortable.sort(items)
    }

    fun setType(sortType: Int, isAscending: Boolean) {
        sortable = sortableByType(sortType)
        sortable.isAscending = isAscending
    }

    fun changeDirection(sortType: Int): Boolean {
        if (sortType == LAST_PUBLISHED_TAG && sortType == sortable.sortId) {
            return false
        }
        val newSortable = sortableByType(sortType)
        if (sortable === newSortable) {
            val ascending = newSortable.isAscending
            newSortable.isAscending = !ascending
        } else {
            sortable.resetDefault()
            sortable = newSortable
        }
        return true
    }

    private fun sortableByType(sortType: Int): PhotoShelfPostSortable = when (sortType) {
        TAG_NAME -> tagNameSortable
        LAST_PUBLISHED_TAG -> lastPublishedTagSortable
        UPLOAD_TIME -> uploadTimeSortable
        else -> throw AssertionError("Invalid $sortType")
    }

    private inner class TagNameSortable(
        isDefaultAscending: Boolean
    ) : PhotoShelfPostSortable(isDefaultAscending, TAG_NAME) {
        override fun sort(items: MutableList<PhotoShelfPost>) {
            items.sortWith { l, r ->
                LastPublishedTimestampComparator.compareTag(
                    l,
                    r,
                    isAscending
                )
            }
        }
    }

    private inner class LastPublishedTagSortable(
        isDefaultAscending: Boolean
    ) : PhotoShelfPostSortable(isDefaultAscending, LAST_PUBLISHED_TAG) {
        override fun sort(items: MutableList<PhotoShelfPost>) {
            items.sortWith(LastPublishedTimestampComparator())
        }
    }

    private inner class UploadTimeSortable(
        isDefaultAscending: Boolean
    ) : PhotoShelfPostSortable(isDefaultAscending, UPLOAD_TIME) {
        override fun sort(items: MutableList<PhotoShelfPost>) {
            items.sortWith { lhs, rhs ->
                val diff = lhs.timestamp - rhs.timestamp
                when (val compare = if (diff < -1) -1 else if (diff > 1) 1 else 0) {
                    0 -> lhs.firstTag.compareTo(rhs.firstTag, true)
                    else -> if (isAscending) compare else -compare
                }
            }
        }
    }

    companion object {
        const val TAG_NAME = 1
        const val LAST_PUBLISHED_TAG = 2
        const val UPLOAD_TIME = 3
    }
}
