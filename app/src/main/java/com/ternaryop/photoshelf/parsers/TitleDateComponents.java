package com.ternaryop.photoshelf.parsers;

import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dave on 15/04/16.
 * Parse the string to extract date components
 */
public class TitleDateComponents {
    private static final String[] months = {"",
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December"};

    int day;
    int month;
    int year = -1;
    Matcher matcher;

    /**
     * Fill parseInfo with day, month, year, matched
     */
    TitleDateComponents(String text) {
        this(text, false);
    }

    TitleDateComponents(String text, boolean swapDayMonth) {
        matcher = extractComponentsFromNumericDate(text);
        if (matcher == null) {
            matcher = extractComponentsFromTextualDate(text);
        }
        fix(swapDayMonth);
    }

    /**
     * handle dates in the form Jan 10, 2010 or January 10 2010 or Jan 15
     * @param text the string to parse
     * @return matcher on success, null otherwise
     */
    private Matcher extractComponentsFromTextualDate(String text) {
        Matcher m = Pattern.compile("\\s?(?:-|,|\\bon\\b)?\\s+\\(?(jan\\w*|feb\\w*|mar\\w*|apr\\w*|may\\w*|jun\\w*|jul\\w*|aug\\w*|sep\\w*|oct\\w*|nov\\w*|dec\\w*)(?!.*(?=jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))[^0-9]*([0-9]*)[^0-9]*([0-9]*)\\)?.*$", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find() && m.groupCount() > 1 && containsDateMatch(m)) {
            int dayIndex = 2;
            int monthIndex = 1;
            int yearIndex = 3;
            month = indexOfMonthFromShort(m.group(monthIndex).toLowerCase(Locale.getDefault()));
            day = m.group(dayIndex).isEmpty() ? 0 : Integer.parseInt(m.group(dayIndex));
            // The date could have the form Febrary 2011 so the day contains the year
            if (day > 2000) {
                year = day;
                day = 0;
            } else {
                if (m.groupCount() == 3 && !m.group(yearIndex).isEmpty()) {
                    year = Integer.parseInt(m.group(yearIndex));
                }
            }
            return m;
        }
        return null;
    }

    /**
     * Check if the matcher contains valid date components
     * @param matcher the matcher
     * @return true if contains date component, false otherwise
     */
    private boolean containsDateMatch(Matcher matcher) {
        String month = matcher.group(1);
        // the month isn't expressed in the short form (jan, dec)
        if (month.length() == 3) {
            return true;
        }
        boolean found = false;
        for (String m : months) {
            if (month.compareToIgnoreCase(m) == 0) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * handle dates in the form dd/dd/dd?? or (dd/dd/??)
     * @param text the string to parse
     * @return matcher on success, null otherwise
     */
    private Matcher extractComponentsFromNumericDate(String text) {
        Matcher m = Pattern.compile("\\s+\\(?([0-9]{1,2})[^\\d]\\s?([0-9]{1,2})[^\\d]\\s?([0-9]{2,4})\\)?").matcher(text);
        if (m.find() && m.groupCount() > 1) {
            day = Integer.parseInt(m.group(1));
            month = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
            return m;
        }
        return null;
    }
    
    private int indexOfMonthFromShort(String shortMonth) {
        for (int i = 0; i < months.length; i++) {
            if (shortMonth.regionMatches(true, 0, months[i], 0, 3)) {
                return i;
            }
        }
        return -1;
    }

    private void fix(boolean swapDayMonth) {
        if (month > 12) {
            swapDayMonth();
        }
        fixYear(Calendar.getInstance().get(Calendar.YEAR));

        // maybe the components format is mm/dd/yyyy so we switch day and month to try dd/mm/yyyy
        if (swapDayMonth || isDateMMDDFormat()) {
            swapDayMonth();
        }
    }

    private boolean isDateMMDDFormat() {
        String strDate = String.format(Locale.US, "%1$04d%2$02d%3$02d", year, month, day);
        String strNow = String.format(Locale.US, "%1$04d%2$02d%3$02d",
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        return strDate.compareTo(strNow) > 0 && month <= 12;
    }

    void swapDayMonth() {
        int tmp = month;
        month = day;
        day = tmp;
    }

    private void fixYear(int yearToUse) {
        // year not found on parsed text
        if (year < 0) {
            year = yearToUse;
        } else {
            // we have a two-digits year
            if (year < 100) {
                year += 2000;
            }
            if (year < 2000) {
                year = yearToUse;
            } else if (year > yearToUse) {
                year = yearToUse;
            }
        }
    }

    String format() {
        StringBuilder sb = new StringBuilder();
        // day could be not present for example "New York City, January 11"
        if (day > 0) {
            sb.append(day).append(" ");
        }
        if (0 < month && month <= months.length) {
            sb.append(months[month]).append(", ");
        }
        sb.append(year);
        return sb.toString();
    }
}
