package com.ternaryop.tumblr;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Blog implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7241228948040188270L;
	
	private String name;
	private String url;
	private String title;
	private boolean primary;

	public Blog() {
		
	}
	
	public Blog(JSONObject jsonResponse) throws JSONException {
        name = jsonResponse.getString("name");
        url = jsonResponse.getString("url");
		title = jsonResponse.getString("title");
		primary = jsonResponse.getBoolean("primary");
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public String getTitle() {
		return title;
	}

	public boolean isPrimary() {
		return primary;
	}
	
	public String getAvatarUrlBySize(int size) {
		return getAvatarUrlBySize(getName(), size);
	}

	public static String getAvatarUrlBySize(CharSequence baseHost, int size) {
		return "http://api.tumblr.com/v2/blog/" + baseHost + ".tumblr.com/avatar/" + size;
	}

	@Override
	public String toString() {
		return name;
	}
}
