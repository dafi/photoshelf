package com.ternaryop.phototumblrshare.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.ternaryop.tumblr.TumblrPost;

import android.content.ContentValues;
import android.provider.BaseColumns;

public class PostTag implements BaseColumns, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5674124483160664227L;
	
	public static final String TAG = "TAG";
	public static final String TUMBLR_NAME = "TUMBLR_NAME";
	public static final String PUBLISH_TIMESTAMP = "PUBLISH_TIMESTAMP";
	public static final String SHOW_ORDER = "SHOW_ORDER";
	
	public static final String[] COLUMNS = new String[] { _ID, TUMBLR_NAME, TAG, PUBLISH_TIMESTAMP, SHOW_ORDER };
	private String tag;
	private long id;
	private long publishTimestamp;
	private long showOrder;
	private String tumblrName;

	public static final String TABLE_NAME = "POST_TAG";
	
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

	public ContentValues getContentValues() {
		ContentValues v = new ContentValues();

		v.put(_ID,  this.id);
		v.put(TUMBLR_NAME, this.tumblrName);
		v.put(TAG, this.tag);
		v.put(PUBLISH_TIMESTAMP, this.publishTimestamp);
		v.put(SHOW_ORDER, this.showOrder);
		
		return v;
	}

	public String getTumblrName() {
		return tumblrName;
	}

	public void setTumblrName(String tumblrName) {
		this.tumblrName = tumblrName;
	}
	
	@Override
	public String toString() {
		return String.format(Locale.US, "%1$s[%2$d] = tag %3$s ts = %4$d order %5$d", tumblrName, id, tag, publishTimestamp, showOrder);
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