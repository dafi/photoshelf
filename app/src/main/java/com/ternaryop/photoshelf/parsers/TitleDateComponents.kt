@file:Suppress("MagicNumber")
package com.ternaryop.photoshelf.parsers

import com.ternaryop.photoshelf.parsers.DateComponents.Companion.MONTH_COUNT
import com.ternaryop.photoshelf.parsers.DateComponents.Companion.YEAR_2000
import com.ternaryop.photoshelf.parsers.DateComponents.Companion.containsDateMatch
import com.ternaryop.photoshelf.parsers.DateComponents.Companion.indexOfMonthFromShort
import com.ternaryop.photoshelf.parsers.DateComponents.Companion.isMonthName
import com.ternaryop.photoshelf.util.date.year
import java.util.Calendar
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

object TextualDateComponents {
    /**
     * handle dates in the form Jan 10, 2010 or January 10 2010 or Jan 15
     * @param text the string to parse
     * @return date components on success, null otherwise
     */
    @Suppress("ComplexMethod")
    fun extract(text: String): DateComponents? {
        val m = Pattern.compile("""\s?(?:[-|,]|\bon\b)?\s+(\d*)(?:st|ns|rd|th)?\s?\(?(jan\w*|feb\w*|mar\w*|apr\w*|may\w*|jun\w*|jul\w*|aug\w*|sep\w*|oct\w*|nov\w*|dec\w*)(?!.*(?=jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec))[^0-9]*([0-9]*)[^0-9]*([0-9]*)\)?.*${'$'}""", Pattern.CASE_INSENSITIVE).matcher(text)
        if (m.find() && m.groupCount() > 1) {
            var dayIndex = 3
            var monthIndex = 2
            var yearIndex = 4
            var expectedGroupCount = 4
            if (containsDateDDMonthYear(m)) {
                dayIndex = 1
                monthIndex = 2
                yearIndex = 3
                expectedGroupCount = 4
            } else if (!containsDateMatch(m)) {
                return null
            }

            val dc = DateComponents()

            dc.month = indexOfMonthFromShort(m.group(monthIndex).toLowerCase(Locale.getDefault()))
            dc.day = if (m.group(dayIndex).isEmpty()) 0 else m.group(dayIndex).toInt()
            // The date could have the form February 2011 so the day contains the year
            if (dc.day > YEAR_2000) {
                dc.year = dc.day
                dc.day = 0
            } else {
                if (m.groupCount() == expectedGroupCount && !m.group(yearIndex).isEmpty()) {
                    dc.year = m.group(yearIndex).toInt()
                }
            }
            dc.datePosition = m.start()
            return dc
        }
        return null
    }

    /**
     * Check if contains date in the form 12th November 2011
     * @param m the m
     * @return true if contains date component, false otherwise
     */
    private fun containsDateDDMonthYear(m: Matcher): Boolean {
        return m.groupCount() == 4
            && !m.group(1).isEmpty()
            && isMonthName(m.group(2))
            && !m.group(3).isEmpty()
            && m.group(4).isEmpty()
    }

}

object NumericDateComponents {
    /**
     * @param text the string to parse
     * @return date components on success, null otherwise
     */
    fun extract(text: String): DateComponents? {
        var dc = extractMostCompleteDate(text)
        if (dc != null) {
            return dc
        }
        dc = extractYear(text)
        if (dc != null) {
            return dc
        }
        return extractIso8601Date(text)
    }

    /**
     * Extract dates in the form dd/dd/dd?? or (dd/dd/??) or (dddd)
     * @return date components on success, null otherwise
     */
    private fun extractMostCompleteDate(text: String): DateComponents? {
        val m = Pattern.compile(""".*\D(\d{1,2})\s*\D\s*(\d{1,2})\s*\D\s*(\d{2}|\d{4})\b""").matcher(text)
        if (m.find() && m.groupCount() > 1) {
            return DateComponents(
                day = m.group(1).toInt(),
                month = m.group(2).toInt(),
                year = m.group(3).toInt(),
                datePosition = m.start(1))
        }
        return null
    }

    /**
     * Extract year starting with 2 (ie. 2ddd)
     * @return date components on success, null otherwise
     */
    private fun extractYear(text: String): DateComponents? {
        val m = Pattern.compile("""\(\s*(2\d{3})\s*\)""").matcher(text)
        if (m.find()) {
            return DateComponents(
                day = -1,
                month = -1,
                year = m.group(1).toInt(),
                datePosition = m.start(1))
        }
        return null
    }

    private fun extractIso8601Date(text: String): DateComponents? {
        val m = Pattern.compile(""".*\D(\d{4})\s*\D\s*(\d{1,2})\s*\D\s*(\d{2})\b""").matcher(text)

        if (m.find()) {
            return DateComponents(
                day = m.group(3).toInt(),
                month = m.group(2).toInt(),
                year = m.group(1).toInt(),
                datePosition = m.start(1))
        }
        return null
    }
}

/**
 * Created by dave on 15/04/16.
 * Parse the string to extract date components
 */
object TitleDateComponents {
    fun extract(text: String,
        swapDayMonth: Boolean = false,
        checkDateInTheFuture: Boolean = false): DateComponents {
        var dc = NumericDateComponents.extract(text)
        if (dc == null) {
            dc = TextualDateComponents.extract(text)
        }
        if (dc == null) {
            return DateComponents(year = Calendar.getInstance().year)
        }
        fix(dc, swapDayMonth, checkDateInTheFuture)
        return dc
    }

    private fun fix(dateComponents: DateComponents, swapDayMonth: Boolean, checkDateInTheFuture: Boolean) {
        if (dateComponents.month > MONTH_COUNT) {
            swapDayMonth(dateComponents)
        }
        dateComponents.fixYear(Calendar.getInstance().year)

        // maybe the components format is mm/dd/yyyy so we switch day and month to try dd/mm/yyyy
        if (swapDayMonth || checkDateInTheFuture && dateComponents.isDateInTheFuture) {
            swapDayMonth(dateComponents)
        }
    }

    private fun swapDayMonth(dateComponents: DateComponents) {
        with (dateComponents) {
            // if day isn't present doesn't swap
            // doesn't generate illegal month value if day > MONTH_COUNT
            if (day == 0 || day > MONTH_COUNT) {
                return
            }
            val tmp = month
            month = day
            day = tmp
        }
    }
}
