package com.ternaryop.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ternaryop.tumblr.TumblrPost;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrException;

public class TumblrUtils {
    public static long getQueueCount(Tumblr tumblr, final String tumblrName) {
        // do not use Tumblr.getQueue() because it creates unused TumblrPost
        String apiUrl = tumblr.getApiUrl(tumblrName, "/posts/queue");
        long count = 0;
        long readCount = 0;

        try {
            Map<String, String> params = new HashMap<String, String>(1);
            do {
                JSONArray arr = tumblr.getConsumer().jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts");
                readCount = arr.length();
                count += readCount;
                params.put("offset", String.valueOf(readCount));
            } while (readCount == 20);
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

    public static List<TumblrPost> getQueueAll(Tumblr tumblr, final String tumblrName) {
        ArrayList<TumblrPost> list = new ArrayList<TumblrPost>();
        long readCount = 0;

        try {
            Map<String, String> params = new HashMap<String, String>(1);
            do {
                List<TumblrPost> queue = tumblr.getQueue(tumblrName, params);
                readCount = queue.size();
                list.addAll(queue);
                params.put("offset", String.valueOf(readCount));
            } while (readCount == 20);
        } catch (Exception e) {
            throw new TumblrException(e);
        }
        return list;
    }
}
