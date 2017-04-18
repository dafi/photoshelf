package com.ternaryop.photoshelf;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by dave on 07/05/15.
 * Helper class to read Http documents from urls
 */
public class HtmlDocumentSupport {
    public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:37.0) Gecko/20100101 Firefox/37.0";

    /**
     * Open connection using the DESKTOP_USER_AGENT
     * @param url the url to open
     * @return the connection
     * @throws IOException
     */
    public static HttpURLConnection openConnection(String url) throws IOException {
        HttpURLConnection connection;
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", HtmlDocumentSupport.DESKTOP_USER_AGENT);
        connection.connect();

        return connection;
    }
}
