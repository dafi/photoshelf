package com.ternaryop.photoshelf.adapter;

import java.util.Comparator;

public class LastPublishedTimestampComparator implements
        Comparator<PhotoShelfPost> {
    @Override
    public int compare(PhotoShelfPost lhs, PhotoShelfPost rhs) {
        long lhsTimestamp = lhs.getLastPublishedTimestamp();
        long rhsTimestamp = rhs.getLastPublishedTimestamp();

        long ldiff = lhsTimestamp - rhsTimestamp;
        int diff = ldiff == 0 ? 0 : ldiff < 0 ? -1 : 1;

        if (diff == 0) {
            String lhsTag = lhs.getFirstTag();
            String rhsTag = rhs.getFirstTag();
            diff = lhsTag.compareToIgnoreCase(rhsTag);
        } else {
            if (lhsTimestamp == Long.MAX_VALUE) {
                return -1;
            }
            if (rhsTimestamp == Long.MAX_VALUE) {
                return 1;
            }
        }

        return diff;
    }
}
