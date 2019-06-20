package com.ternaryop.feedly

import okhttp3.Headers
import java.net.HttpURLConnection

/**
 * Created by dave on 25/02/17.
 * Hold the info about rate limits
 */

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
            return formatResetLimit(apiResetLimit)
        }

    init {
        reset()
    }

    private fun formatResetLimit(seconds: Int): String {
        val sb = StringBuilder()
        var remainingSeconds = seconds
        val days = remainingSeconds / (24 * 3600)
        remainingSeconds %= (24 * 3600)
        val hours = remainingSeconds / 3600
        remainingSeconds %= 3600
        val minutes = remainingSeconds / 60
        remainingSeconds %= 60

        if (days > 0) {
            sb.append(" $days days ")
        }
        return sb.append(String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)).toString()
    }

    private fun reset() {
        apiCallsCount = -1
        apiResetLimit = -1
    }

    fun update(responseCode: Int, headers: Headers) {
        synchronized(this) {
            reset()
            if (responseCode == HttpURLConnection.HTTP_OK) {
                apiCallsCount = headers["X-Ratelimit-Count"]?.toInt() ?: -1
                apiResetLimit = headers["X-Ratelimit-Reset"]?.toInt() ?: -1
            }
        }
    }
}
