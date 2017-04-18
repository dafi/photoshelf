package com.ternaryop.photoshelf.birthday;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by dave on 01/04/17.
 * Image Extractor Manager
 */

public class BirthdayManager {
    public static final String API_PREFIX = "http://visualdiffer.com/image";

    private String accessToken;

    public BirthdayManager(String accessToken) {
        this.accessToken = accessToken;
    }

    public BirthdayInfo search(final String name) throws Exception {
        StringBuilder sb = new StringBuilder(API_PREFIX + "/v1/birthday/search?name=" + URLEncoder.encode(name, "UTF-8"));

        HttpURLConnection conn = null;
        try {
            conn = getSignedGetConnection(sb.toString());
            handleError(conn);
            final JSONArray birthdayInfo = toJson(conn.getInputStream()).getJSONArray("birthdays");
            return new BirthdayInfo(birthdayInfo.getJSONObject(0));
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    protected HttpURLConnection getSignedGetConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setRequestProperty("PhotoShelf-Subscription-Key", accessToken);
        conn.setRequestMethod("GET");

        return conn;
    }

    public JSONObject toJson(InputStream is) throws Exception {
        try (InputStream bis = new BufferedInputStream(is)) {
            return JSONUtils.jsonFromInputStream(bis);
        }
    }

    protected void handleError(HttpURLConnection conn) throws Exception {
        if (conn.getResponseCode() == 200) {
            return;
        }
        final JSONObject error = toJson(conn.getErrorStream());
        throw new RuntimeException("Error " + conn.getResponseCode() + ": " + error.getString("errorMessage"));
    }
}
