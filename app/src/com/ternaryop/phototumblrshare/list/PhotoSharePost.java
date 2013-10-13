package com.ternaryop.phototumblrshare.list;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.ternaryop.phototumblrshare.DraftPostHelper;
import com.ternaryop.tumblr.TumblrPhotoPost;

public class PhotoSharePost extends TumblrPhotoPost {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
	private long lastPublishedTimestamp;

	public enum ScheduleTime {
		POST_PUBLISH_NEVER,
		POST_PUBLISH_FUTURE,
		POST_PUBLISH_PAST
	};
	
	public PhotoSharePost(TumblrPhotoPost photoPost, long lastPublishedTimestamp) {
		super(photoPost);
		this.lastPublishedTimestamp = lastPublishedTimestamp;
	}

	/**
	 * The last published time can be in the future if the post is scheduled
	 * @return
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
		long days = DraftPostHelper.daysSinceTimestamp(tt);
		String daysString = DraftPostHelper.formatPublishDaysAgo(tt);
		if (days != Long.MAX_VALUE && days > 0) {
			String string = DATE_FORMAT.format(new Date(tt));
        	daysString += " (" + string + ")";
		}
		return daysString;
	}
}
