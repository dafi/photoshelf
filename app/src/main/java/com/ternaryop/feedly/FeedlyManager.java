package com.ternaryop.feedly;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private String accessToken;
    private final String userId;
    private String refreshToken;

    public FeedlyManager(String accessToken, String userId, String refreshToken) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.refreshToken = refreshToken;
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
            handleError(conn);
            final JSONArray items = toJson(conn.getInputStream()).getJSONArray("items");
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
            conn = getSignedPostConnection(API_PREFIX + "/v3/markers", "application/json", data);
            FeedlyRateLimit.instance.update(conn);
            handleError(conn);
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    public String refreshAccessToken(String clientId, String clientSecret) throws Exception {
        HttpURLConnection conn = null;
        try {
            String data = "refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8")
                    + "&client_id=" + URLEncoder.encode(clientId, "UTF-8")
                    + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8")
                    + "&grant_type=" + URLEncoder.encode("refresh_token", "UTF-8");

            conn = getSignedPostConnection(API_PREFIX + "/v3/auth/token", "application/x-www-form-urlencoded", data);
            FeedlyRateLimit.instance.update(conn);

            handleError(conn);
            return toJson(conn.getInputStream()).getString("access_token");
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Exception ignored) {}
        }
    }

    private HttpURLConnection getSignedPostConnection(String url, String contentType, String data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "OAuth " + accessToken);
        conn.setRequestProperty("Content-Type", contentType);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(data.getBytes(StandardCharsets.UTF_8));
        }

        return conn;
    }

    private HttpURLConnection getSignedGetConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();
        conn.setRequestProperty("Authorization", "OAuth " + accessToken);
        conn.setRequestMethod("GET");

        return conn;
    }

    public JSONObject toJson(InputStream is) throws Exception {
        try (InputStream bis = new BufferedInputStream(is)) {
            return JSONUtils.jsonFromInputStream(bis);
        }
    }

    private void handleError(HttpURLConnection conn) throws Exception {
        if (conn.getResponseCode() == 200) {
            return;
        }
        final JSONObject error = toJson(conn.getErrorStream());
        throw new RuntimeException("Error " + conn.getResponseCode() + ": " + error.getString("errorMessage"));
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
