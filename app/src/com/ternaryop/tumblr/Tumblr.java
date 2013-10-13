package com.ternaryop.tumblr;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.ternaryop.utils.DialogUtils;

public class Tumblr {
	public final static int MAX_POST_PER_REQUEST = 20;
	private final static String API_PREFIX = "http://api.tumblr.com/v2";

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

    private void addPostsToList(ArrayList<TumblrPost> list, JSONArray arr) throws JSONException {
		for (int i = 0; i < arr.length(); i++) {
			list.add(build(arr.getJSONObject(i)));
		}
    }
    
    public List<TumblrPost> getDraftPosts(final String tumblrName) {
        String apiUrl = getApiUrl(tumblrName, "/posts/draft");
		ArrayList<TumblrPost> list = new ArrayList<TumblrPost>();

		try {
    		JSONObject json = consumer.jsonFromGet(apiUrl);
    		JSONArray arr = json.getJSONObject("response").getJSONArray("posts");
    		
    		while (arr.length() > 0) {
    			addPostsToList(list, arr);
    			long beforeId = arr.getJSONObject(arr.length() - 1).getLong("id");
    	        List<NameValuePair> params = new ArrayList<NameValuePair>(1);
    	        params.add(new BasicNameValuePair("before_id", beforeId + ""));

    	        arr = consumer.jsonFromGet(apiUrl, params).getJSONObject("response").getJSONArray("posts");
    		}
		} catch (Exception e) {
			throw new TumblrException(e);
		}
		return list;
    }

    public List<TumblrPost> getQueue(final String tumblrName, Map<String, String> params) {
        String apiUrl = getApiUrl(tumblrName, "/posts/queue");
		ArrayList<TumblrPost> list = new ArrayList<TumblrPost>();

		try {
	        List<NameValuePair> nameValuePairs = mapToNameValuePair(params);
			JSONObject json = consumer.jsonFromGet(apiUrl, nameValuePairs);
			JSONArray arr = json.getJSONObject("response").getJSONArray("posts");
			addPostsToList(list, arr);
		} catch (Exception e) {
			throw new TumblrException(e);
		}
		return list;
    }
    
    public List<TumblrPhotoPost> getPhotoPosts(final String tumblrName, Map<String, String> params) {
        String apiUrl = getApiUrl(tumblrName, "/posts/photo");
		ArrayList<TumblrPhotoPost> list = new ArrayList<TumblrPhotoPost>();

		try {
	        List<NameValuePair> nameValuePairs = mapToNameValuePair(params);
	        nameValuePairs.add(new BasicNameValuePair("api_key", consumer.getConsumerKey()));

	        JSONObject json = consumer.jsonFromGet(apiUrl, nameValuePairs);
			JSONArray arr = json.getJSONObject("response").getJSONArray("posts");
			long totalPosts = json.getJSONObject("response").has("total_posts") ? json.getJSONObject("response").getLong("total_posts") : -1; 
			for (int i = 0; i < arr.length(); i++) {
				TumblrPhotoPost post = (TumblrPhotoPost)build(arr.getJSONObject(i));
				if (totalPosts != -1) {
					post.setTotalPosts(totalPosts);
				}
				list.add(post);
			}
		} catch (Exception e) {
			throw new TumblrException(e);
		}

		return list;
    }

	private List<NameValuePair> mapToNameValuePair(Map<String, String> params) {
		if (params == null || params.size() == 0) {
			return Collections.emptyList();
		}
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		Iterator<String> itr = params.keySet().iterator();
		while (itr.hasNext()) {
			String key = itr.next();
		    nameValuePairs.add(new BasicNameValuePair(key, params.get(key)));
		}
		return nameValuePairs;
	}
    
    public void publishPost(final String tumblrName, final long id, final Callback<JSONObject> callback) {
        new AsyncTask<Void, Void, JSONObject>() {
        	Exception error;
    		@Override
    		protected JSONObject doInBackground(Void... params) {
    	        String apiUrl = getApiUrl(tumblrName, "/post/edit");
    	    	
    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    	        nameValuePairs.add(new BasicNameValuePair("id", id + ""));
    	        nameValuePairs.add(new BasicNameValuePair("state", "published"));

    	        try {
        	        JSONObject json = consumer.jsonFromPost(apiUrl, nameValuePairs);
        	        return json.getJSONObject("response");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(JSONObject response) {
				if (error == null) {
					callback.complete(instance, response);
				} else {
					callback.failure(instance, error);
				}
			}
        }.execute();
    }

    public void schedulePost(final String tumblrName, final long id, final long timestamp, final Callback<JSONObject> callback) {
        new AsyncTask<Void, Void, JSONObject>() {
        	Exception error;
    		@Override
    		protected JSONObject doInBackground(Void... params) {
    	        String apiUrl = getApiUrl(tumblrName, "/post/edit");
    	        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    	        String gmtDate = dateFormat.format(new Date(timestamp));
    	    	
    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
    	        nameValuePairs.add(new BasicNameValuePair("id", id + ""));
    	        nameValuePairs.add(new BasicNameValuePair("state", "queue"));
    	        nameValuePairs.add(new BasicNameValuePair("publish_on", gmtDate));

    	        try {
        	        JSONObject json = consumer.jsonFromPost(apiUrl, nameValuePairs);
        	        return json.getJSONObject("response");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(JSONObject response) {
				if (error == null) {
					callback.complete(instance, response);
				} else {
					callback.failure(instance, error);
				}
			}
        }.execute();
    }

	public static TumblrPost build(JSONObject json) throws JSONException {
		String type = json.getString("type");
		
		if (type.equals("photo")) {
			return new TumblrPhotoPost(json);
		}
		throw new IllegalArgumentException("Unable to build post for type " + type);
	}
}
