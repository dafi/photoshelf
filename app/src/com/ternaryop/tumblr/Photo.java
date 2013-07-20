package com.ternaryop.tumblr;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;

public class Photo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2592188558410774527L;
	private int width;
	private int height;
	private String url;
	private Bitmap bitmap;

	public Photo(JSONObject json) throws JSONException {
		width = json.getInt("width");
		height = json.getInt("height");
		url = json.getString("url");
	}

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getUrl() {
		return url;
	}
}

