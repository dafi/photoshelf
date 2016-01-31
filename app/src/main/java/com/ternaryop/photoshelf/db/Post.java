package com.ternaryop.photoshelf.db;

import java.io.Serializable;
import java.util.Locale;

public class Post implements Serializable {
    /**
     *
     */

    private long id;

    private long blogId;
    private long tagId;
    private long publishTimestamp;
    private long showOrder;

    public Post() {
    }

    public Post(long id, long blogId, long tagId, long timestamp, long showOrder) {
        this.id = id;
        this.blogId = blogId;
        this.tagId = tagId;
        this.publishTimestamp = timestamp;
        this.showOrder = showOrder;
    }
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBlogId() {
        return blogId;
    }

    public void setBlogId(long blogId) {
        this.blogId = blogId;
    }

    public long getTagId() {
        return tagId;
    }

    public void setTagId(long tagId) {
        this.tagId = tagId;
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

    @Override
    public String toString() {
        return String.format(Locale.US, "%1$d[%2$d] = tag %3$d ts = %4$d order %5$d",
                blogId, id, tagId, publishTimestamp, showOrder);
    }
}