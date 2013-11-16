package com.ternaryop.tumblr;

import org.json.JSONException;
import org.json.JSONObject;

public class TumblrAltSize {
    private int width;
    private int height;
    private String url;
    
    public TumblrAltSize() {
	}

    public TumblrAltSize(JSONObject json) throws JSONException {
    	url = json.getString("url");
		width = json.getInt("width");
		height = json.getInt("height");
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return width + "x" + height;
	}
}
