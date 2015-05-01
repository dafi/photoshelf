package com.ternaryop.photoshelf.adapter;

import java.util.Comparator;

import org.joda.time.DateTimeComparator;

/**
 * Sort using the order from top to bottom shown below
 * 1. Never Published
 * 2. Older published
 * 3. In the future (ie scheduled)
 */
public class LastPublishedTimestampComparator implements
        Comparator<PhotoShelfPost> {
    @Override
    public int compare(PhotoShelfPost lhs, PhotoShelfPost rhs) {
        long lhsTimestamp = lhs.getLastPublishedTimestamp();
        long rhsTimestamp = rhs.getLastPublishedTimestamp();

        if (lhsTimestamp == rhsTimestamp) {
            return compareTag(lhs, rhs, true);
        }
        // never published item goes on top
        if (lhsTimestamp == Long.MAX_VALUE) {
            return -1;
        }
        if (rhsTimestamp == Long.MAX_VALUE) {
            return 1;
        }
        long now = System.currentTimeMillis();
        if (lhsTimestamp > now && rhsTimestamp > now) {
            return lhsTimestamp < rhsTimestamp ? -1 : 1;
        }
        // item in the future goes to bottom
        if (lhsTimestamp > now) {
            return 1;
        }
        if (rhsTimestamp > now) {
            return -1;
        }

        // compare only the date part
        int compare = DateTimeComparator.getDateOnlyInstance().compare(lhsTimestamp, rhsTimestamp);
        if (compare == 0) {
            compare = compareTag(lhs, rhs, true);
        }
        return compare;
    }

    public static int compareTag(PhotoShelfPost lhs, PhotoShelfPost rhs, boolean ascending) {
        int compare = lhs.getFirstTag().compareToIgnoreCase(rhs.getFirstTag());
        if (compare == 0) {
            long diff = lhs.getTimestamp() - rhs.getTimestamp();
            // always ascending
            return diff < -1 ? -1 : diff > 1 ? 1 : 0;
        }
        return ascending ? compare : -compare;
    }
}
