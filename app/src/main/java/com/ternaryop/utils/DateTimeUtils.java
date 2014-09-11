package com.ternaryop.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by dave on 08/06/14.
 * Contains methods to compute days, years
 */
public class DateTimeUtils {
    public static int yearsBetweenDates(Calendar from, Calendar to) {
        if (from.after(to)) {
            Calendar temp = to;
            to = from;
            from = temp;
        }
        int years = to.get(Calendar.YEAR) - from.get(Calendar.YEAR);

        if (years != 0) {
            // increment months by 1 because are zero based
            int fromSpan = (from.get(Calendar.MONTH) + 1) * 100 + from.get(Calendar.DAY_OF_MONTH);
            int toSpan = (to.get(Calendar.MONTH) + 1) * 100 + to.get(Calendar.DAY_OF_MONTH);

            if (toSpan < fromSpan) {
                --years;
            }
        }

        return years;
    }

    /**
     * Determine days difference since timestamp to current time
     * if timestamp is equal to Long.MAX_VALUE then return Long.MAX_VALUE
     *
     * @param timestamp the start milliseconds from which start to count days
     * @return numbers of days, if negative indicates days in the future beyond
     * passed timestamp
     */
    public static long daysSinceTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long nowTime = cal.getTimeInMillis();
        long dayTime = 24 * 60 * 60 * 1000;
        long days;

        if (timestamp == Long.MAX_VALUE) {
            days = Long.MAX_VALUE;
        } else {
            cal = Calendar.getInstance();
            cal.setTime(new Date(timestamp));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            long tsWithoutTime = cal.getTimeInMillis();
            long spanTime = nowTime - tsWithoutTime;
            days = spanTime / dayTime;
        }
        return days;
    }

    public static String formatPublishDaysAgo(long timestamp) {
        long days = daysSinceTimestamp(timestamp);
        String dayString;

        if (days == Long.MAX_VALUE) {
            dayString = "Never Published";
        } else {
            if (days < 0) {
                if (days == -1) {
                    dayString = new SimpleDateFormat("'Tomorrow at' HH:mm", Locale.US).format(new Date(timestamp));
                } else {
                    dayString = new SimpleDateFormat("'In " + (-days) + " days at' HH:mm", Locale.US).format(new Date(timestamp));
                }
            } else if (days == 0) {
                dayString = new SimpleDateFormat("'Today at' HH:mm", Locale.US).format(new Date(timestamp));
            } else if (days == 1) {
                dayString = "Yesterday";
            } else {
                dayString = days + " days ago";
            }
        }
        return dayString;
    }
}
