package com.ternaryop.feedly;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ternaryop.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by dave on 24/02/17.
 * Feedly Manager
 */

public class FeedlyManager {
    public static final String API_PREFIX = "https://cloud.feedly.com";

    private final String accessToken;
    private final String userId;

    public FeedlyManager(String accessToken, String userId) {
        this.accessToken = accessToken;
        this.userId = userId;
    }

    public String getGlobalSavedTag() {
        return String.format("user/%s/tag/global.saved", userId);
    }

    public List<FeedlyContent> getStreamContents(final String streamId, final int count, final long newerThan, final String continuation) throws Exception {
        StringBuilder sb = new StringBuilder(API_PREFIX + "/v3/streams/contents?streamId=" + streamId);

        if (count > 0) {
            sb.append("&count=").append(count);
        }
        if (newerThan > 0) {
            sb.append("&newerThan=").append(newerThan);
        }
        if (continuation != null) {
            sb.append("&continuation=").append(continuation);
        }
        HttpURLConnection conn = null;
        try {
            conn = getSignedGetConnection(sb.toString());
            final JSONArray items = jsonFromGet(conn).getJSONArray("items");
            final ArrayList<FeedlyContent> list = new ArrayList<>(items.length());
            for (int i = 0; i < items.length(); i++) {
                list.add(new FeedlyContent(items.getJSONObject(i)));
            }
            FeedlyRateLimit.instance.update(conn);
            return list;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    public void markSaved(List<String> ids, boolean saved) throws Exception {
        if (ids.isEmpty()) {
            return;
        }
        Map<String, Object> map = new HashMap<>();

        map.put("type", "entries");
        map.put("action", saved? "markAsSaved" : "markAsUnsaved");
        map.put("entryIds", ids);

        final String data = JSONUtils.toJSON(map).toString();

        HttpURLConnection conn = null;
        try {
            conn = getSignedPostConnection(API_PREFIX + "/v3/markers", data);
            FeedlyRateLimit.instance.update(conn);

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Error " + conn.getResponseCode() + ": " + conn.getResponseMessage());
            }
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }

    }

    private HttpURLConnection getSignedPostConnection(String url, String data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "OAuth " + accessToken);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes("UTF-8"));
        }

        return conn;
    }

    private HttpURLConnection getSignedGetConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "OAuth " + accessToken);
        conn.setRequestMethod("GET");

        return conn;
    }

    public JSONObject jsonFromGet(HttpURLConnection conn) throws Exception {
        try (InputStream in = new BufferedInputStream(conn.getInputStream())) {
            return JSONUtils.jsonFromInputStream(in);
        }
    }
}
