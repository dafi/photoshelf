package com.ternaryop.photoshelf.parsers

import com.ternaryop.utils.date.dayOfMonth
import com.ternaryop.utils.date.month
import com.ternaryop.utils.date.year
import java.util.Calendar
import java.util.Locale
import java.util.regex.Matcher

/**
 * Hold the date components day, month, year
 */
@Suppress("MagicNumber")
class DateComponents(var day: Int = 0, var month: Int = 0, var year: Int = -1, var datePosition: Int = -1,
    var format: Int = UNKNOWN_FORMAT) {
    val isDateInTheFuture: Boolean
        get() {
            if (month > MONTH_COUNT) {
                return false
            }
            if (day < 0 && month < 0) {
                return false
            }
            val strDate = String.format(Locale.US, "%1$04d%2$02d%3$02d", year, month, day)
            val calendar = Calendar.getInstance()
            val strNow = String.format(Locale.US, "%1$04d%2$02d%3$02d",
                calendar.year,
                calendar.month + 1,
                calendar.dayOfMonth)
            return strDate > strNow
        }

    fun format(): String {
        val sb = StringBuilder()
        // day could be not present for example "New York City, January 11"
        if (day > 0) {
            sb.append(day).append(" ")
        }
        if (0 < month && month <= months.size) {
            sb.append(months[month]).append(", ")
        }
        sb.append(year)
        return sb.toString()
    }

    fun fixYear(yearToUse: Int) {
        // year not found on parsed text
        if (year < 0) {
            year = yearToUse
        } else {
            // we have a two-digits year
            if (year < 100) {
                year += YEAR_2000
            }
            if (year < YEAR_2000) {
                year = yearToUse
            } else if (year > yearToUse) {
                year = yearToUse
            }
        }
    }

    companion object {
        const val MONTH_COUNT = 12
        const val MONTH_NAME_SHORT_LEN = 3
        const val YEAR_2000 = 2000
        const val UNKNOWN_FORMAT = 0
        const val NUMERIC_FORMAT = 1
        const val TEXTUAL_FORMAT = 2

        /**
         * Check if the matcher contains valid date components
         * @param matcher the matcher
         * @return true if contains date component, false otherwise
         */
        fun containsDateMatch(matcher: Matcher): Boolean {
            val month = matcher.group(2)
            // the month isn't expressed in the short form (jan, dec)
            return month.length == MONTH_NAME_SHORT_LEN || isMonthName(month)
        }

        fun indexOfMonthFromShort(shortMonth: String): Int {
            return months.indices.firstOrNull {
                shortMonth.regionMatches(0, months[it], 0, MONTH_NAME_SHORT_LEN, ignoreCase = true) }
                ?: -1
        }

        fun isMonthName(month: String): Boolean = months.any { month.compareTo(it, ignoreCase = true) == 0 }

        val months = arrayOf(
            "",
            "January", "February", "March",
            "April", "May", "June",
            "July", "August", "September",
            "October", "November", "December")
    }
}