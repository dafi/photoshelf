package com.ternaryop.photoshelf.feedly.adapter

import android.content.Context
import com.ternaryop.photoshelf.util.sort.AbsSortable

/**
 * Created by dave on 10/03/18.
 * Hold sort types supported by Feedly Viewer
 */
typealias FeedContentDelegateSortable = AbsSortable<FeedlyContentDelegate>

class FeedlyContentSortSwitcher(val context: Context) {
    private val titleNameSortable: FeedContentDelegateSortable by lazy {
        object : FeedContentDelegateSortable(true, TITLE_NAME) {
            override fun sort(items: MutableList<FeedlyContentDelegate>) =
                items.sortWith(Comparator { c1, c2 -> c1.compareTitle(c2) })
        }
    }

    private val saveTimestampSortable: FeedContentDelegateSortable by lazy {
        object : FeedContentDelegateSortable(true, SAVED_TIMESTAMP) {
            override fun sort(items: MutableList<FeedlyContentDelegate>) =
                items.sortWith(Comparator { c1, c2 -> c1.compareActionTimestamp(c2) })
        }
    }

    private val lastPublishTimestampSortable: FeedContentDelegateSortable by lazy {
        object : FeedContentDelegateSortable(true, LAST_PUBLISH_TIMESTAMP) {
            override fun sort(items: MutableList<FeedlyContentDelegate>) {
                items.sortWith(Comparator { c1, c2 -> c1.compareLastTimestamp(c2) })
            }
        }
    }

    var currentSortable = titleNameSortable
        private set

    fun setType(sortType: Int) {
        currentSortable = when (sortType) {
            TITLE_NAME -> titleNameSortable
            SAVED_TIMESTAMP -> saveTimestampSortable
            LAST_PUBLISH_TIMESTAMP -> lastPublishTimestampSortable
            else -> throw AssertionError("Invalid $sortType")
        }
    }

    fun sort(items: MutableList<FeedlyContentDelegate>) {
        currentSortable.sort(items)
    }

    companion object {
        const val TITLE_NAME = 1
        const val SAVED_TIMESTAMP = 2
        const val LAST_PUBLISH_TIMESTAMP = 3
    }
}
