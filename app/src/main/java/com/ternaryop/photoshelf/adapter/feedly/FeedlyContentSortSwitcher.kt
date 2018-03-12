package com.ternaryop.photoshelf.adapter.feedly

import android.content.Context
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.util.sort.AbsSortable

/**
 * Created by dave on 10/03/18.
 * Hold sort types supported by Feedly Viewer
 */
typealias FeedContentDelegateSortable = AbsSortable<FeedlyContentDelegate>

class FeedlyContentSortSwitcher(val context: Context, tumblrName: String) {
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
                updateLastPublishTimestamp(items)
                items.sortWith(Comparator { c1, c2 -> c1.compareLastTimestamp(c2) })
            }

            private fun updateLastPublishTimestamp(items: MutableList<FeedlyContentDelegate>) {
                val titles = HashSet<String>(items.size)
                for (fc in items) {
                    // replace any no no-break space with whitespace
                    // see http://www.regular-expressions.info/unicode.html for \p{Zs}
                    titles.add(fc.title.replace("""\p{Zs}""".toRegex(), " "))
                    fc.lastPublishTimestamp = java.lang.Long.MIN_VALUE
                }
                val list = DBHelper.getInstance(context).postTagDAO
                    .getListPairLastPublishedTimestampTag(titles, tumblrName)
                for (fc in items) {
                    val title = fc.title
                    for (timeTag in list) {
                        if (timeTag.second.regionMatches(0, title, 0, timeTag.second.length, ignoreCase = true)) {
                            fc.lastPublishTimestamp = timeTag.first
                        }
                    }
                }
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
