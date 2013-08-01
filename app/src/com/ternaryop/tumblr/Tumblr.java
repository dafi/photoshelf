package com.ternaryop.tumblr;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ternaryop.utils.DialogUtils;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class Tumblr {
	private final String API_PREFIX = "http://api.tumblr.com/v2";
    public static final int POST_PER_REQUEST = 50;

	private static Tumblr instance;
	
	private TumblrHttpOAuthConsumer consumer;

	public Tumblr(TumblrHttpOAuthConsumer consumer) {
		this.consumer = consumer;
	}

	public static void getTumblr(final Activity activity, final Callback<Void> callback) {
		if (instance != null) {
			callback.complete(instance, null);
			return;
		}
		TumblrHttpOAuthConsumer.login(activity, new Callback<Void>() {
			@Override
			public void failure(Tumblr tumblr, Exception e) {
				DialogUtils.showErrorDialog(activity, e);
			}
			
			@Override
			public void complete(Tumblr tumblr, Void result) {
				Tumblr.instance = tumblr;
				callback.complete(tumblr, result);
			}
		});
	}

    public void getBlogList(final Callback<Blog[]> callback) {
        new AsyncTask<Void, Void, Blog[]>() {

			@Override
    		protected Blog[] doInBackground(Void... params) {
    	        String apiUrl = API_PREFIX + "/user/info";

    	        try {
    	        	JSONObject json = consumer.jsonFromGet(apiUrl);
        	        JSONArray jsonBlogs = json.getJSONObject("response").getJSONObject("user").getJSONArray("blogs");
        	        Blog[] blogs = new Blog[jsonBlogs.length()];
        	        for (int i = 0; i < jsonBlogs.length(); i++) {
						blogs[i] = new Blog(jsonBlogs.getJSONObject(i));
					}
        	        return blogs;
				} catch (Exception e) {
					// TODO: handle exception
					Log.e("err", "list", e);
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(Blog[] blogs) {
				callback.complete(instance, blogs);
			}
        }.execute();
    }
    
    
    protected String getApiUrl(String tumblrName, String suffix) {
        return API_PREFIX + "/blog/" + tumblrName + ".tumblr.com" + suffix;
    }
    
    public void draftPhotoPost(final String tumblrName, final String url, final String caption, final String tags, final Callback<Long> callback) {
    	createPhotoPost(tumblrName, url, caption, tags, "draft", callback);
    }

    public void publishPhotoPost(final String tumblrName, final String url, final String caption, final String tags, final Callback<Long> callback) {
    	createPhotoPost(tumblrName, url, caption, tags, "published", callback);
    }

    protected void createPhotoPost(final String tumblrName, final String url, final String caption, final String tags, final String state, final Callback<Long> callback) {
        new AsyncTask<Void, Void, Long>() {
        	Exception error;
    		@Override
    		protected Long doInBackground(Void... params) {
    	        String apiUrl = getApiUrl(tumblrName, "/post");

    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
    	        nameValuePairs.add(new BasicNameValuePair("state", state));
    	        nameValuePairs.add(new BasicNameValuePair("type", "photo"));
    	        nameValuePairs.add(new BasicNameValuePair("source", url));
    	        nameValuePairs.add(new BasicNameValuePair("caption", caption));
    	        nameValuePairs.add(new BasicNameValuePair("tags", tags));
    	        try {

    	        	JSONObject json = consumer.jsonFromPost(apiUrl, nameValuePairs);
        	        return json.getJSONObject("response").getLong("id");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(Long postId) {
				if (error == null) {
					callback.complete(instance, postId);
				} else {
					callback.failure(instance, error);
				}
			}
        }.execute();
    }
}
