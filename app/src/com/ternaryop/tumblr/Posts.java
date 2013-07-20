package com.ternaryop.tumblr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;


public class Posts implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -911234126959790209L;
	private ArrayList<Post> posts;

	public Posts() {
		posts = new ArrayList<Post>();
	}

	public Posts(JSONArray jsonPosts) throws JSONException {
		posts = new ArrayList<Post>();
		addJSONPosts(jsonPosts);
	}
	
	public void addJSONPosts(JSONArray jsonPosts) throws JSONException {
		for (int i = 0; i < jsonPosts.length(); i++) {
			posts.add(new Post(jsonPosts.getJSONObject(i)));
		}
	}

	public void addAll(Posts posts) {
		this.posts.addAll(posts.getPosts());
	}

	public List<Post> getPosts() {
		return posts;
	}
	
	public int size() {
		return posts.size();
	}
}