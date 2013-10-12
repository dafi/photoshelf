package com.ternaryop.phototumblrshare.db;

import java.io.Serializable;

import android.content.ContentValues;
import android.provider.BaseColumns;

public class PostTag implements BaseColumns, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 423293675220882505L;

	public static final String TABLE_NAME = "POST_TAG";
	
	public static final String POST_ID = "POST_ID";
	public static final String TUMBLR_NAME = "TUMBLR_NAME";
	public static final String TAG = "TAG";
	public static final String PUBLISH_TIMESTAMP = "PUBLISH_TIMESTAMP";
	public static final String SHOW_ORDER = "SHOW_ORDER";
	public static final String POST_ID_TYPE = "POST_ID_TYPE";
	
	public static final String POST_TYPE_PUBLISHED = "p";
	public static final String POST_TYPE_SCHEDULED = "s";
	
	private long postId;
	private String tumblrName;
	private String tag;
	private long publishTimestamp;
	private long showOrder;
	private String postIdType;
	
	public static final String[] COLUMNS = new String[] { POST_ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER, POST_ID_TYPE };
	
	public PostTag() {
	}
	
	public PostTag(long postId, String tumblrName, String tag,
			long publishTimestamp, long showOrder, String postIdType) {
		this.postId = postId;
		this.tumblrName = tumblrName;
		this.tag = tag;
		this.publishTimestamp = publishTimestamp;
		this.showOrder = showOrder;
		this.postIdType = postIdType;
	}
	
	public long getPostId() {
		return postId;
	}

	public void setPostId(long postId) {
		this.postId = postId;
	}

	public String getTumblrName() {
		return tumblrName;
	}

	public void setTumblrName(String tumblrName) {
		this.tumblrName = tumblrName;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public long getPublishTimestamp() {
		return publishTimestamp;
	}

	public void setPublishTimestamp(long publishTimestamp) {
		this.publishTimestamp = publishTimestamp;
	}

	public long getShowOrder() {
		return showOrder;
	}

	public void setShowOrder(long showOrder) {
		this.showOrder = showOrder;
	}

	public String getPostIdType() {
		return postIdType;
	}

	public void setPostIdType(String postIdType) {
		this.postIdType = postIdType;
	}

	public ContentValues getContentValues() {
		ContentValues v = new ContentValues();

		v.put(POST_ID, this.postId);
		v.put(TUMBLR_NAME, this.tumblrName);
		v.put(TAG, this.tag);
		v.put(PUBLISH_TIMESTAMP, this.publishTimestamp);
		v.put(SHOW_ORDER, this.showOrder);
		v.put(POST_ID_TYPE, this.postIdType);
		
		return v;
	}
}
