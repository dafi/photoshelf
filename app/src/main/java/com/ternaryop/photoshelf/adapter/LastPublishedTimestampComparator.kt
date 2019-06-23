package com.ternaryop.photoshelf.adapter

import com.ternaryop.utils.date.millisToLocalDate

/**
 * Sort using the order from top to bottom shown below
 * 1. Never Published
 * 2. Older published
 * 3. In the future (ie scheduled)
 */
class LastPublishedTimestampComparator : Comparator<PhotoShelfPost> {
    @Suppress("ComplexMethod")
    override fun compare(lhs: PhotoShelfPost, rhs: PhotoShelfPost): Int {
        val lhsTimestamp = lhs.lastPublishedTimestamp
        val rhsTimestamp = rhs.lastPublishedTimestamp

        if (lhsTimestamp == rhsTimestamp) {
            return compareTag(lhs, rhs, true)
        }
        // never published item goes on top
        if (lhsTimestamp == java.lang.Long.MAX_VALUE) {
            return -1
        }
        if (rhsTimestamp == java.lang.Long.MAX_VALUE) {
            return 1
        }
        val now = System.currentTimeMillis()
        if (lhsTimestamp > now && rhsTimestamp > now) {
            return if (lhsTimestamp < rhsTimestamp) -1 else 1
        }
        // item in the future goes to bottom
        if (lhsTimestamp > now) {
            return 1
        }
        if (rhsTimestamp > now) {
            return -1
        }

        // compare only the date part
        var compare = lhsTimestamp.millisToLocalDate().compareTo(rhsTimestamp.millisToLocalDate())
        if (compare == 0) {
            compare = compareTag(lhs, rhs, true)
        }
        return compare
    }

    companion object {

        fun compareTag(lhs: PhotoShelfPost, rhs: PhotoShelfPost, ascending: Boolean): Int {
            val compare = lhs.firstTag.compareTo(rhs.firstTag, ignoreCase = true)
            if (compare == 0) {
                val diff = lhs.timestamp - rhs.timestamp
                // always ascending
                return if (diff < -1) -1 else if (diff > 1) 1 else 0
            }
            return if (ascending) compare else -compare
        }
    }
}
