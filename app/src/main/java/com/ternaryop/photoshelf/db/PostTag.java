package com.ternaryop.photoshelf.db;

import java.util.Locale;

public class PostTag extends Post {
    /**
     * 
     */
    private static final long serialVersionUID = 5674124483160664227L;
    
    private String tag;
    private long id;
    private long publishTimestamp;
    private long showOrder;
    private String tumblrName;

    public PostTag() {
    }

    public PostTag(long id, String tumblrName, String tag, long timestamp, long showOrder) {
        this.tumblrName = tumblrName;
        this.tag = tag.toLowerCase(Locale.US);
        this.id = id;
        this.publishTimestamp = timestamp;
        this.showOrder = showOrder;
    }

    public PostTag(Post post) {
        this.id = post.getId();
        this.publishTimestamp = post.getPublishTimestamp();
        this.showOrder = post.getShowOrder();
    }
    
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag.toLowerCase(Locale.US);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getPublishTimestamp() {
        return publishTimestamp;
    }

    public void setPublishTimestamp(long timestamp) {
        this.publishTimestamp = timestamp;
    }

    public long getShowOrder() {
        return showOrder;
    }

    public void setShowOrder(long showOrder) {
        this.showOrder = showOrder;
    }

    public String getTumblrName() {
        return tumblrName;
    }

    public void setTumblrName(String tumblrName) {
        this.tumblrName = tumblrName;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%1$s[%2$d] = tag %3$s ts = %4$d order %5$d",
                tumblrName, id, tag, publishTimestamp, showOrder);
    }
}