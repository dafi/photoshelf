package com.ternaryop.phototumblrshare.db;

import java.io.Serializable;

public class LastPublishedPostCache implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 423293675220882505L;

	private long postId;
	private String tumblrName;
	private String tag;
	private long publishTimestamp;
	private long showOrder;
	private String postIdType;

	public LastPublishedPostCache() {
	}
	
	public LastPublishedPostCache(long postId, String tumblrName, String tag,
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

}
