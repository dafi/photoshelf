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
    int datePosition;

    /**
     * Fill parseInfo with day, month, year, matched
     */
    TitleDateComponents(String text) {
        this(text, false, false);
    }

    TitleDateComponents(String text, boolean swapDayMonth, boolean checkDateInTheFuture) {
        datePosition = extractComponentsFromNumericDate(text);
        if (datePosition < 0) {
            datePosition = extractComponentsFromTextualDate(text);
        }
        fix(swapDayMonth, checkDateInTheFuture);
    }

    /**
     * handle dates in the form Jan 10, 2010 or January 10 2010 or Jan 15
     * @param text the string to parse
     * @return the date position on success, -1 otherwise
     */
    private int extractComponentsFromTextualDate(String text) {
        Matcher m = Pattern.compile("\\s?(?:-|,|\\bon\\b)?\\s+(\\d*)(?:st|ns|rd|th)?\\s?\\(?(jan\\w*|feb\\w*|mar\\w*|apr\\w*|may\\w*|jun\\w*|jul\\w*|aug\\w*|sep\\w*|oct\\w*|nov\\w*|dec\\w*)(?!.*(?=jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))[^0-9]*([0-9]*)[^0-9]*([0-9]*)\\)?.*$", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find() && m.groupCount() > 1) {
            int dayIndex = 3;
            int monthIndex = 2;
            int yearIndex = 4;
            int expectedGroupCount = 4;
            if (containsDateDDMonthYear(m)) {
                dayIndex = 1;
                monthIndex = 2;
                yearIndex = 3;
                expectedGroupCount = 4;
            } else if (!containsDateMatch(m)) {
                return -1;
            }

            month = indexOfMonthFromShort(m.group(monthIndex).toLowerCase(Locale.getDefault()));
            day = m.group(dayIndex).isEmpty() ? 0 : Integer.parseInt(m.group(dayIndex));
            // The date could have the form Febrary 2011 so the day contains the year
            if (day > 2000) {
                year = day;
                day = 0;
            } else {
                if (m.groupCount() == expectedGroupCount && !m.group(yearIndex).isEmpty()) {
                    year = Integer.parseInt(m.group(yearIndex));
                }
            }
            return m.start();
        }
        return -1;
    }

    /**
     * Check if contains date in the form 12th November 2011
     * @param m the m
     * @return true if contains date component, false otherwise
     */
    private boolean containsDateDDMonthYear(Matcher m) {
        return  m.groupCount() == 4 && !m.group(1).isEmpty() && isMonthName(m.group(2)) && !m.group(3).isEmpty() && m.group(4).isEmpty();
    }


    /**
     * Check if the matcher contains valid date components
     * @param matcher the matcher
     * @return true if contains date component, false otherwise
     */
    private boolean containsDateMatch(Matcher matcher) {
        String month = matcher.group(2);
        // the month isn't expressed in the short form (jan, dec)
        return month.length() == 3 || isMonthName(month);
    }

    private boolean isMonthName(String month) {
        for (String m : months) {
            if (month.compareToIgnoreCase(m) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * handle dates in the form dd/dd/dd?? or (dd/dd/??) or (dddd)
     * @param text the string to parse
     * @return matcher on success, null otherwise
     */
    private int extractComponentsFromNumericDate(String text) {
        Matcher m = Pattern.compile(".*\\D(\\d{1,2})\\s*\\D\\s*(\\d{1,2})\\s*\\D\\s*(\\d{2}|\\d{4})\\b").matcher(text);
        if (m.find() && m.groupCount() > 1) {
            day = Integer.parseInt(m.group(1));
            month = Integer.parseInt(m.group(2));
            year = Integer.parseInt(m.group(3));
            return m.start(1);
        }
        // only year
        m = Pattern.compile("\\(\\s*(2\\d{3})\\s*\\)").matcher(text);
        if (m.find()) {
            day = -1;
            month = -1;
            year = Integer.parseInt(m.group(1));
            return m.start(1);
        }
        return -1;
    }
    
    private int indexOfMonthFromShort(String shortMonth) {
        for (int i = 0; i < months.length; i++) {
            if (shortMonth.regionMatches(true, 0, months[i], 0, 3)) {
                return i;
            }
        }
        return -1;
    }

    private void fix(boolean swapDayMonth, boolean checkDateInTheFuture) {
        if (month > 12) {
            swapDayMonth();
        }
        fixYear(Calendar.getInstance().get(Calendar.YEAR));

        // maybe the components format is mm/dd/yyyy so we switch day and month to try dd/mm/yyyy
        if (swapDayMonth || (checkDateInTheFuture && isDateInTheFuture())) {
            swapDayMonth();
        }
    }

    private boolean isDateInTheFuture() {
        if (month > 12) {
            return false;
        }
        if (day < 0 && month < 0) {
            return false;
        }
        String strDate = String.format(Locale.US, "%1$04d%2$02d%3$02d", year, month, day);
        String strNow = String.format(Locale.US, "%1$04d%2$02d%3$02d",
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        return strDate.compareTo(strNow) > 0;
    }

    void swapDayMonth() {
        // if day isn't present doesn't swap
        // doesn't generate illegal month value if day > 12
        if (day == 0 || day > 12) {
            return;
        }
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
