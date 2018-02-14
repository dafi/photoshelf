package com.ternaryop.photoshelf.util.html

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by dave on 07/05/15.
 * Helper class to read Http documents from urls
 */
@Suppress("MemberVisibilityCanBePrivate")
object HtmlDocumentSupport {
    const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:57.0) Gecko/20100101 Firefox/57.0"

    /**
     * Open connection using the DESKTOP_USER_AGENT
     * @param url the url to open
     * @return the connection
     * @throws IOException when open fails
     */
    @Throws(IOException::class)
    fun openConnection(url: String): HttpURLConnection {
        val connection: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", HtmlDocumentSupport.DESKTOP_USER_AGENT)
        connection.connect()

        return connection
    }
}
