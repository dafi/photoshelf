package com.ternaryop.utils;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrException;

public class TumblrUtils {
    public static long getQueueCount(Tumblr tumblr, final String tumblrName) {
        String apiUrl = tumblr.getApiUrl(tumblrName, "/posts/queue");
        long count = 0;

        try {
            JSONObject json = tumblr.getConsumer().jsonFromGet(apiUrl);
            JSONArray arr = json.getJSONObject("response").getJSONArray("posts");
            count = arr.length();
        } catch (Exception e) {
            throw new TumblrException(e);
        }
        return count;
    }
    
    
    public static long getDraftCount(Tumblr tumblr, String tumblrName) {
        String apiUrl = tumblr.getApiUrl(tumblrName, "/posts/draft");
        long count = 0;

        try {
            JSONObject json = tumblr.getConsumer().jsonFromGet(apiUrl);
            JSONArray arr = json.getJSONObject("response").getJSONArray("posts");
            
            Map<String, String> params = new HashMap<String, String>(1);
            while (arr.length() > 0) {
                count += arr.length();
                long beforeId = arr.getJSONObject(arr.length() - 1).getLong("id");
                params.put("before_id", beforeId + "");

                arr = tumblr.getConsumer().jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts");
            }
        } catch (Exception e) {
            throw new TumblrException(e);
        }
        return count;
    }
}
