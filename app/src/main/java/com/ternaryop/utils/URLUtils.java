package com.ternaryop.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLUtils {
    /* Resolve a possible shorten url to the original one */
    public static String resolveShortenURL(String strURL) {
        URLConnection conn;
        try {
            URL inputURL = new URL(strURL);
            conn = inputURL.openConnection();
            conn.getHeaderFields();
            return conn.getURL().toString();
        } catch (Exception e) {
            return strURL;
        }
    }

    /**
     * Handle the case the url is redirected to a location with different protocol (eg from http to https or viceversa)
     * @param url the url to open
     * @return the open connection
     * @throws java.io.IOException
     */
    public static HttpURLConnection openConnectionFollowingDifferentProtocols(String url) throws IOException {
        HttpURLConnection conn;
        String location;
        URL next;

        while (true) {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);

            switch (conn.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    location = conn.getHeaderField("Location");
                    // Deal with relative URLs
                    next = new URL(new URL(url), location);
                    url = next.toExternalForm();
                    continue;
            }
            break;
        }
        return conn;
    }

}
