package com.ternaryop.feedly

import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder
import java.io.IOException
import java.net.HttpURLConnection

/**
 * Created by dave on 25/02/17.
 * Hold the info about rate limits
 */

private var resetLimitFormat = PeriodFormatterBuilder()
        .appendDays()
        .appendSeparator(" days ")
        .printZeroAlways().minimumPrintedDigits(2).appendHours()
        .appendSeparator(":")
        .printZeroAlways().minimumPrintedDigits(2).appendMinutes()
        .appendSeparator(":")
        .printZeroAlways().minimumPrintedDigits(2).appendSeconds()
        .toFormatter()

@Suppress("MemberVisibilityCanBePrivate")
object FeedlyRateLimit {

    var apiCallsCount: Int = -1
        private set
    var apiResetLimit: Int = -1
        private set

    val apiResetLimitAsString: String
        get() {
            if (apiResetLimit < 0) {
                return "N/A"
            }
            val period = Period.seconds(apiResetLimit).normalizedStandard()
            return resetLimitFormat.print(period)
        }

    init {
        reset()
    }

    private fun reset() {
        apiCallsCount = -1
        apiResetLimit = -1
    }

    @Throws(IOException::class)
    fun update(conn: HttpURLConnection) {
        synchronized(this) {
            reset()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return
            }
            apiCallsCount = conn.getHeaderField("X-Ratelimit-Count")?.toInt() ?: -1
            apiResetLimit = conn.getHeaderField("X-Ratelimit-Reset")?.toInt() ?: -1
        }
    }
}
