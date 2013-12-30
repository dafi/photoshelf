package com.ternaryop.photoshelf.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ternaryop.tumblr.TumblrPost;

public class PostTag implements Serializable {
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
	
	public static List<PostTag> postTagsFromTumblrPost(TumblrPost tumblrPost) {
		int showOrder = 1;
		ArrayList<PostTag> list = new ArrayList<PostTag>();

		for (String tag : tumblrPost.getTags()) {
			list.add(new PostTag(tumblrPost.getPostId(), tumblrPost.getBlogName(), tag, tumblrPost.getTimestamp(), showOrder));
			++showOrder;
		}
		
		return list;
	}
}