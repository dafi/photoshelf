package com.ternaryop.photoshelf.extractor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONObject;

/**
 * Created by dave on 01/04/17.
 * Image Extractor Manager
 */

public class ImageExtractorManager {
//    public static final String API_PREFIX = "http://10.0.3.2/image";
    public static final String API_PREFIX = "http://visualdiffer.com/image";
//    public static final String API_PREFIX = "http://192.168.0.2/image";

    private final String accessToken;

    public ImageExtractorManager(String accessToken) {
        this.accessToken = accessToken;
    }

    public ImageGallery getGallery(final String url) throws Exception {
        StringBuilder sb = new StringBuilder(API_PREFIX + "/v1/extract/gallery?url=" + URLEncoder.encode(url, "UTF-8"));

        HttpURLConnection conn = null;
        try {
            conn = getSignedGetConnection(sb.toString());
            handleError(conn);
            final JSONObject gallery = toJson(conn.getInputStream()).getJSONObject("gallery");
            return new ImageGallery(gallery);
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    public String getImageUrl(final String url) throws Exception {
        StringBuilder sb = new StringBuilder(API_PREFIX + "/v1/extract/image?url=" + URLEncoder.encode(url, "UTF-8"));

        HttpURLConnection conn = null;
        try {
            conn = getSignedGetConnection(sb.toString());
            handleError(conn);
            return toJson(conn.getInputStream()).getString("imageUrl");
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
