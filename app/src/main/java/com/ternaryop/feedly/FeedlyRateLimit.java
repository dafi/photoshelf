package com.ternaryop.feedly;

import java.io.IOException;
import java.net.HttpURLConnection;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Created by dave on 25/02/17.
 * Hold the info about rate limits
 */

public class FeedlyRateLimit {
    public static final PeriodFormatter RESET_LIMIT_FORMAT = new PeriodFormatterBuilder()
            .appendDays()
            .appendSeparator(" days ")
            .printZeroAlways().minimumPrintedDigits(2).appendHours()
            .appendSeparator(":")
            .printZeroAlways().minimumPrintedDigits(2).appendMinutes()
            .appendSeparator(":")
            .printZeroAlways().minimumPrintedDigits(2).appendSeconds()
            .toFormatter();

    private int apiCallsCount;
    private int apiResetLimit;

    public static final FeedlyRateLimit instance = new FeedlyRateLimit();

    private FeedlyRateLimit() {
        reset();
    }

    private void reset() {
        apiCallsCount = -1;
        apiResetLimit = -1;
    }

    public void update(HttpURLConnection conn) throws IOException {
        synchronized (this) {
            reset();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }
            String v = conn.getHeaderField("X-Ratelimit-Count");
            if (v != null) {
                apiCallsCount = Integer.parseInt(v);
            }
            v = conn.getHeaderField("X-Ratelimit-Reset");
            if (v != null) {
                apiResetLimit = Integer.parseInt(v);
            }
        }
    }

    public int getApiCallsCount() {
        return apiCallsCount;
    }

    public int getApiResetLimit() {
        return apiResetLimit;
    }

    public String getApiResetLimitAsString() {
        if (apiResetLimit < 0) {
            return "N/A";
        }
        Period period = Period.seconds(apiResetLimit).normalizedStandard();
        return RESET_LIMIT_FORMAT.print(period);
    }
}
