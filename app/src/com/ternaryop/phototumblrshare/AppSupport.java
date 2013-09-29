package com.ternaryop.phototumblrshare;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class AppSupport {
	private final Context context;
	private static final String PREFS_NAME = "tumblrShareImage";
	private static final String PREF_SELECTED_BLOG = "selectedBlog";
	private static final String PREF_BLOG_NAMES = "blogNames";

	public AppSupport(Context context) {
		this.context = context;
	}
	
	public String getSelectedBlogName() {
		SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, 0);
		return preferences.getString(PREF_SELECTED_BLOG, null);
	}
	
	public void setSelectedBlogName(String blogName) {
		Editor edit = context.getSharedPreferences(PREFS_NAME, 0)
				.edit();
		edit.putString(PREF_SELECTED_BLOG, blogName);
		edit.commit();
	}
	
	public Set<String> getBlogList() {
		SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, 0);
		return preferences.getStringSet(PREF_BLOG_NAMES, null);
	}
	
	public void setBlogList(List<String> blogNames) {
		Editor edit = context.getSharedPreferences(PREFS_NAME, 0).edit();
		edit.putStringSet(PREF_BLOG_NAMES, new HashSet<String>(blogNames));
		edit.commit();
	}
}
