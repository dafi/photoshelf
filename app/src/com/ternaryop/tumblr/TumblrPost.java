package com.ternaryop.tumblr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TumblrPost implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 9136359874716067522L;
    private String blogName;
    private long postId;
    private String postUrl;
    private String type;
    private long timestamp;
    private String date;
    private String format;
    private String reblogKey;
    private List<String> tags;
    private boolean bookmarklet;
    private boolean mobile;
    private String sourceUrl;
    private String sourceTitle;
    private boolean liked;
    private String state;
    private long totalPosts;
    private long noteCount;
    
    // queue posts
    private long scheduledPublishTime;
    
    public TumblrPost() {
    }
    
    public TumblrPost(JSONObject json) throws JSONException {
        blogName = json.getString("blog_name");
        postId = json.getLong("id");
        postUrl = json.getString("post_url");
        type = json.getString("type");
        timestamp = json.getLong("timestamp");
        date = json.getString("date");
        format = json.getString("format");
        reblogKey = json.getString("reblog_key");
        bookmarklet = json.has("bookmarklet") && json.getBoolean("bookmarklet");
        mobile = json.has("mobile") && json.getBoolean("mobile");
        sourceUrl = json.has("source_url") ? json.getString("source_url") : null;
        sourceTitle = json.has("source_title") ? json.getString("source_title") : null;
        liked = json.has("liked") && json.getBoolean("liked");
        state = json.getString("state");
        totalPosts = json.has("total_posts") ? json.getLong("total_posts") : 0;
        noteCount = json.has("note_count") ? json.getLong("note_count") : 0;

        JSONArray jsonTags = json.getJSONArray("tags");
        tags = new ArrayList<String>(jsonTags.length());
        for (int i = 0; i < jsonTags.length(); i++) {
            tags.add(jsonTags.getString(i));
        }
        
        if (json.has("scheduled_publish_time")) {
            scheduledPublishTime = json.getLong("scheduled_publish_time");
        }
    }

    public TumblrPost(TumblrPost post) {
        blogName = post.blogName;
        postId = post.postId;
        postUrl = post.postUrl;
        type = post.type;
        timestamp = post.timestamp;
        date = post.date;
        format = post.format;
        reblogKey = post.reblogKey;
        bookmarklet = post.bookmarklet;
        mobile = post.mobile;
        sourceUrl = post.sourceUrl;
        sourceTitle = post.sourceTitle;
        liked = post.liked;
        state = post.state;
        totalPosts = post.totalPosts;

        tags = post.tags;
        scheduledPublishTime = post.getScheduledPublishTime();
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getReblogKey() {
        return reblogKey;
    }

    public void setReblogKey(String reblogKey) {
        this.reblogKey = reblogKey;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setTags(String tags) {
        ArrayList<String> list = new ArrayList<String>();
        for (String s : tags.split(",")) {
            list.add(s.trim());
        }
        setTags(list);
    }
    
    public boolean isBookmarklet() {
        return bookmarklet;
    }

    public void setBookmarklet(boolean bookmarklet) {
        this.bookmarklet = bookmarklet;
    }

    public boolean isMobile() {
        return mobile;
    }

    public void setMobile(boolean mobile) {
        this.mobile = mobile;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getTotalPosts() {
        return totalPosts;
    }

    public void setTotalPosts(long totalPosts) {
        this.totalPosts = totalPosts;
    }

    public long getScheduledPublishTime() {
        return scheduledPublishTime;
    }

    public void setScheduledPublishTime(long scheduledPublishTime) {
        this.scheduledPublishTime = scheduledPublishTime;
    }
    
    public String getTagsAsString() {
        if (tags == null || tags.size() == 0) {
            return "";
        }
        return TextUtils.join(",", tags);
    }

    public long getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(long noteCount) {
        this.noteCount = noteCount;
    }
}
