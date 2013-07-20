package com.ternaryop.tumblr;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

public class Blog implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7241228948040188270L;
	private int totalBlogPosts;
	private Posts posts = new Posts();
	private int postsCount;
	
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
	
	public Posts updateFromJSON(JSONObject jsonResponse) throws JSONException {
		postsCount = jsonResponse.getInt("total_posts");
		totalBlogPosts = jsonResponse.getJSONObject("blog").getInt("posts");
		Posts postsInResponse = new Posts(jsonResponse.getJSONArray("posts"));
		posts.addAll(postsInResponse);
		
		return postsInResponse;
	}

	public int getTotalBlogPosts() {
		return totalBlogPosts;
	}

	public Posts getPosts() {
		return posts;
	}

	public int getPostsCount() {
		return postsCount;
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
	
	@Override
	public String toString() {
		return name;
	}
}
