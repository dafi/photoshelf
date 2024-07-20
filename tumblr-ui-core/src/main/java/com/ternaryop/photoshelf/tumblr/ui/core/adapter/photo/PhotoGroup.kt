package com.ternaryop.photoshelf.tumblr.ui.core.adapter.photo

import android.util.Range
import com.ternaryop.photoshelf.tumblr.ui.core.adapter.PhotoShelfPost

/**
 * Created by dave on 10/03/18.
 * Helper util to group photos by id
 */
object PhotoGroup {
    fun calcGroupIds(items: List<PhotoShelfPost>) {
        if (items.isEmpty()) {
            return
        }
        val count = items.size
        var groupId = 0
        var last = items[0].firstTag
        items[0].groupId = groupId

        var i = 1
        while (i < count) {
            // set same groupId for all identical tags
            while (i < count && items[i].firstTag.equals(last, ignoreCase = true)) {
                items[i++].groupId = groupId
            }
            if (i < count) {
                ++groupId
                items[i].groupId = groupId
                last = items[i].firstTag
            }
            i++
        }
    }

    fun getRangeFromPosition(items: List<PhotoShelfPost>, position: Int, groupId: Int): Range<Int>? {
        if (position < 0) {
            return null
        }
        var min = position
        var max = position

        while (min > 0 && items[min - 1].groupId == groupId) {
            --min
        }

        val lastIndex = items.size - 1
        while (max < lastIndex && items[max].groupId == groupId) {
            ++max
        }

        // group is empty
        return if (min == max) {
            null
        } else {
            Range.create(min, max)
        }
    }
}
