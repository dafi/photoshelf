package com.ternaryop.photoshelf.db;

import java.io.Serializable;

import com.ternaryop.tumblr.TumblrPost;

public class TumblrPostCache implements Serializable {
    /**
     *
     */

    public final static int CACHE_TYPE_DRAFT = 0;

    private String id;
    private String blogName;
    private int cacheType;
    private long timestamp;
    private TumblrPost post;

    public TumblrPostCache() {
    }

    public TumblrPostCache(String id, TumblrPost post, int cacheType) {
        this.id = id;
        this.blogName = post.getBlogName();
        this.cacheType = cacheType;
        this.timestamp = post.getTimestamp();
        this.post = post;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBlogName() {
        return blogName;
    }

    public void setBlogName(String blogName) {
        this.blogName = blogName;
    }

    public int getCacheType() {
        return cacheType;
    }

    public void setCacheType(int cacheType) {
        this.cacheType = cacheType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public TumblrPost getPost() {
        return post;
    }

    public void setPost(TumblrPost post) {
        this.post = post;
    }
}