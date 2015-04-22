package com.ternaryop.photoshelf.adapter;

import com.ternaryop.tumblr.TumblrPhotoPost;
import com.ternaryop.utils.DateTimeUtils;

public class PhotoShelfPost extends TumblrPhotoPost {
    /**
     * 
     */
    private static final long serialVersionUID = -6670033021694674250L;
    private long lastPublishedTimestamp;
    private int groupId;

    public enum ScheduleTime {
        POST_PUBLISH_NEVER,
        POST_PUBLISH_FUTURE,
        POST_PUBLISH_PAST
    }
    
    public PhotoShelfPost(TumblrPhotoPost photoPost, long lastPublishedTimestamp) {
        super(photoPost);
        this.lastPublishedTimestamp = lastPublishedTimestamp;
    }

    /**
     * The last published time can be in the future if the post is scheduled
     * @return the last published time
     */
    public long getLastPublishedTimestamp() {
        return lastPublishedTimestamp;
    }

    public void setLastPublishedTimestamp(long lastPublishedTimestamp) {
        this.lastPublishedTimestamp = lastPublishedTimestamp;
    }
    
    public ScheduleTime getScheduleTimeType() {
        if (lastPublishedTimestamp == Long.MAX_VALUE) {
            return ScheduleTime.POST_PUBLISH_NEVER;
        } else if (lastPublishedTimestamp > System.currentTimeMillis()) {
            return ScheduleTime.POST_PUBLISH_FUTURE;
        } else {
            return ScheduleTime.POST_PUBLISH_PAST;
        }
    }
    
    public String getLastPublishedTimestampAsString() {
        long tt = getScheduledPublishTime() > 0 ? getScheduledPublishTime() * 1000 : lastPublishedTimestamp;
        return DateTimeUtils.formatPublishDaysAgo(tt, DateTimeUtils.APPEND_DATE_FOR_PAST_AND_PRESENT);
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
    
    /**
     * Protect against IndexOutOfBoundsException returning an empty string
     * @return the first tag or an empty string
     */
    public String getFirstTag() {
        return getTags().size() > 0 ? getTags().get(0) : "";
    }
}
