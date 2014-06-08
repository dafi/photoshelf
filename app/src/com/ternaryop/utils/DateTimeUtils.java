package com.ternaryop.utils;

import java.util.Calendar;

/**
 * Created by dave on 08/06/14.
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
}
