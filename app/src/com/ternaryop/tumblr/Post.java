package com.ternaryop.tumblr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Post implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5659232324251719980L;
	private List<Photo> photos;
	private List<String> tags;
	private long id;
	private long timestamp;
	
	public Post(JSONObject jsonPost) throws JSONException {
		id = jsonPost.getLong("id");
		timestamp = jsonPost.getLong("timestamp");
		
		// tags
		JSONArray jsonTags = jsonPost.getJSONArray("tags");
		tags = new ArrayList<String>(jsonTags.length());
		for (int i = 0; i < jsonTags.length(); i++) {
			tags.add(jsonTags.getString(i));
		}

		// images
		JSONArray jsonPhotos = jsonPost.getJSONArray("photos");
		JSONObject jsonPhoto = jsonPhotos.getJSONObject(0);
		JSONArray jsonAltSizes = jsonPhoto.getJSONArray("alt_sizes");
		ArrayList<Photo> list = new ArrayList<Photo>();
		for (int i = 0; i < jsonAltSizes.length(); i++) {
			list.add(new Photo(jsonAltSizes.getJSONObject(i)));
		}
		photos = list;
	}

	public List<Photo> getPhotos() {
		return photos;
	}

	public List<String> getTags() {
		return tags;
	}

	public long getId() {
		return id;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
    public Photo getClosestPhotoByWidth(int width) {
    	for (Photo p : photos) {
            // some images don't have the exact (==) width so we get closest width (<=)
			if (p.getWidth() <= width) {
				return p;
			}
		}
        return null;
    }
	
}
