package com.ternaryop.photoshelf.customsearch;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Locale;

import android.text.TextUtils;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONObject;

/**
 * Created by dave on 30/04/17.
 * Mini Client interface for Google Custom Search (CSE)
 * https://cse.google.com/cse/all
 * https://developers.google.com/custom-search/json-api/v1/using_rest
 */

public class GoogleCustomSearchClient {
    public static final String API_PREFIX = "https://www.googleapis.com/customsearch";

    private final String apiKey;
    private final String cx;

    public GoogleCustomSearchClient(String apiKey, String cx) {
        this.apiKey = apiKey;
        this.cx = cx;
    }

    public CustomSearchResult search(final String q) throws Exception {
        return search(q, null);
    }

    public CustomSearchResult search(final String q, final String[] fields) throws Exception {
        return new CustomSearchResult(getJSON(q, fields));
    }

    public String getCorrectedQuery(final String q) throws Exception {
        return CustomSearchResult.getCorrectedQuery(getJSON(q, new String[] {"spelling"}));
    }

    private JSONObject getJSON(final String q, final String[] fields) throws Exception {
        String url = String.format(Locale.US, "%s/v1?key=%s&cx=%s&q=%s",
                API_PREFIX,
                apiKey,
                cx,
                URLEncoder.encode(q, "UTF-8"));
        if (fields != null) {
            url += "&fields=" + TextUtils.join(",", fields);
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setRequestMethod("GET");
            return toJson(conn.getInputStream());
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    public JSONObject toJson(InputStream is) throws Exception {
        try (InputStream bis = new BufferedInputStream(is)) {
            return JSONUtils.jsonFromInputStream(bis);
        }
    }
}
