package com.ternaryop.tumblr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.ternaryop.photoshelf.db.DBHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public class TumblrUtils {
    public static long getQueueCount(Tumblr tumblr, final String tumblrName) {
        // do not use Tumblr.getQueue() because it creates unused TumblrPost
        String apiUrl = tumblr.getApiUrl(tumblrName, "/posts/queue");
        long count = 0;
        long readCount;

        try {
            Map<String, String> params = new HashMap<String, String>(1);
            do {
                JSONArray arr = tumblr.getConsumer().jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts");
                readCount = arr.length();
                count += readCount;
                params.put("offset", String.valueOf(count));
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
        long readCount;

        try {
            Map<String, String> params = new HashMap<String, String>(1);
            do {
                List<TumblrPost> queue = tumblr.getQueue(tumblrName, params);
                readCount = queue.size();
                list.addAll(queue);
                params.put("offset", String.valueOf(list.size()));
            } while (readCount == 20);
        } catch (Exception e) {
            throw new TumblrException(e);
        }
        return list;
    }

    public static int renameTag(String fromTag, String toTag, Context context, String blogName) {
        HashMap<String, String> searchParams = new HashMap<String, String>();
        searchParams.put("type", "photo");
        searchParams.put("tag", fromTag);
        int offset = 0;
        boolean loadNext;
        Tumblr tumblr = Tumblr.getSharedTumblr(context);
        HashMap<String, String> params = new HashMap<>();
        int renamedCount = 0;

        do {
            searchParams.put("offset", String.valueOf(offset));
            List<TumblrPost> postsList = tumblr.getPublicPosts(blogName, searchParams);
            loadNext = postsList.size() > 0;
            offset += postsList.size();

            for (TumblrPost post : postsList) {
                if (replaceTag(fromTag, toTag, post)) {
                    params.clear();
                    params.put("id", String.valueOf(post.getPostId()));
                    params.put("tags", post.getTagsAsString());
                    tumblr.editPost(blogName, params);
                    updateTagsOnDB(post.getPostId(), post.getTagsAsString(), context, blogName);
                    ++renamedCount;
                }
            }
        } while (loadNext);
        return renamedCount;
    }

    private static boolean replaceTag(String fromTag, String toTag, TumblrPost post) {
        ArrayList<String> renamedTag = new ArrayList<>(post.getTags());
        boolean found = false;
        for (int i = 0; i < renamedTag.size(); i++) {
            if (renamedTag.get(i).equalsIgnoreCase(fromTag)) {
                renamedTag.remove(i);
                renamedTag.add(i, toTag);
                found = true;
                break;
            }
        }
        if (found) {
            post.setTags(renamedTag);
        }
        return found;
    }

    private static void updateTagsOnDB(long id, String tags, Context context, String blogName) {
        final HashMap<String, String> newValues = new HashMap<>();
        newValues.put("id", String.valueOf(id));
        newValues.put("tags", tags);
        newValues.put("tumblrName", blogName);
        DBHelper.getInstance(context).getPostDAO().update(newValues, context);
    }
}
