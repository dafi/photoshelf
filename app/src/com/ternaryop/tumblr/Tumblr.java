package com.ternaryop.tumblr;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

public class Tumblr {
	public final static int MAX_POST_PER_REQUEST = 20;
	private final static String API_PREFIX = "http://api.tumblr.com/v2";

	private static Tumblr instance;
	
	private TumblrHttpOAuthConsumer consumer;

	private Tumblr(TumblrHttpOAuthConsumer consumer) {
		this.consumer = consumer;
	}

	public static Tumblr getSharedTumblr(Context context) {
		if (instance == null) {
			instance = new Tumblr(new TumblrHttpOAuthConsumer(context));
		}
		return instance;
	}
	
	public static boolean isLogged(Context context) {
	    return TumblrHttpOAuthConsumer.isLogged(context);
	}

	public static void login(Context context) {
		TumblrHttpOAuthConsumer.loginWithActivity(context);
	}

	public static boolean handleOpenURI(final Context context, final Uri uri, AuthenticationCallback callback) {
		return TumblrHttpOAuthConsumer.handleOpenURI(context, uri, callback);
	}
	
    public void getBlogList(final Callback<Blog[]> callback) {
        new AsyncTask<Void, Void, Blog[]>() {
        	Exception error;

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
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(Blog[] blogs) {
				if (error == null) {
					callback.complete(blogs);
				} else {
					callback.failure(error);
				}
			}
        }.execute();
    }
    
    
    protected String getApiUrl(String tumblrName, String suffix) {
        return API_PREFIX + "/blog/" + tumblrName + ".tumblr.com" + suffix;
    }
    
    public void draftPhotoPost(final String tumblrName, final Object urlOrFile, final String caption, final String tags, final Callback<Long> callback) {
    	createPhotoPost(tumblrName, urlOrFile, caption, tags, "draft", callback);
    }

    public void draftPhotoPost(final String tumblrName, final Object urlOrFile, final String caption, final String tags) {
        try {
            createPhotoPost(tumblrName, urlOrFile, caption, tags, "draft");
        } catch (JSONException e) {
            throw new TumblrException(e);
        }
    }
    
    public void publishPhotoPost(final String tumblrName, final Object urlOrFile, final String caption, final String tags, final Callback<Long> callback) {
    	createPhotoPost(tumblrName, urlOrFile, caption, tags, "published", callback);
    }

    public void publishPhotoPost(final String tumblrName, final Object urlOrFile, final String caption, final String tags) {
        try {
            createPhotoPost(tumblrName, urlOrFile, caption, tags, "published");
        } catch (JSONException e) {
            throw new TumblrException(e);
        }
    }
    
    protected void createPhotoPost(final String tumblrName, final Object urlOrFile, final String caption, final String tags, final String state, final Callback<Long> callback) {
        new AsyncTask<Void, Void, Long>() {
            Exception error;
            @Override
            protected Long doInBackground(Void... voidParams) {
                try {
                    return createPhotoPost(tumblrName, urlOrFile, caption, tags, state);
                } catch (Exception e) {
                    error = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Long postId) {
                if (error == null) {
                    callback.complete(postId);
                } else {
                    callback.failure(error);
                }
            }
        }.execute();
    }

    protected long createPhotoPost(final String tumblrName, final Object urlOrFile, final String caption, final String tags, final String state) throws JSONException {
        String apiUrl = getApiUrl(tumblrName, "/post");
        HashMap<String, Object> params = new HashMap<String, Object>();

        if (urlOrFile instanceof String) {
            params.put("source", urlOrFile);
        } else {
            params.put("data", urlOrFile);
        }
        params.put("state", state);
        params.put("type", "photo");
        params.put("caption", caption);
        params.put("tags", tags);
        
        JSONObject json = consumer.jsonFromPost(apiUrl, params);
        return json.getJSONObject("response").getLong("id");
    }

    public static void addPostsToList(ArrayList<TumblrPost> list, JSONArray arr) throws JSONException {
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
    		
            Map<String, String> params = new HashMap<String, String>(1);
    		while (arr.length() > 0) {
    			addPostsToList(list, arr);
    			long beforeId = arr.getJSONObject(arr.length() - 1).getLong("id");
    	        params.put("before_id", beforeId + "");

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
            JSONObject json = consumer.jsonFromGet(apiUrl, params);
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
		    Map<String, String> paramsWithKey = new HashMap<String, String>(params);
		    paramsWithKey.put("api_key", consumer.getConsumerKey());

	        JSONObject json = consumer.jsonFromGet(apiUrl, paramsWithKey);
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

    public void publishPost(final String tumblrName, final long id, final Callback<JSONObject> callback) {
        new AsyncTask<Void, Void, JSONObject>() {
        	Exception error;
    		@Override
    		protected JSONObject doInBackground(Void... voidParams) {
    	        String apiUrl = getApiUrl(tumblrName, "/post/edit");
    	    	
                Map<String, String> params = new HashMap<String, String>();
    	        params.put("id", id + "");
    	        params.put("state", "published");

    	        try {
        	        JSONObject json = consumer.jsonFromPost(apiUrl, params);
        	        return json.getJSONObject("response");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(JSONObject response) {
				if (error == null) {
					callback.complete(response);
				} else {
					callback.failure(error);
				}
			}
        }.execute();
    }

    public void deletePost(final String tumblrName, final long id, final Callback<JSONObject> callback) {
        new AsyncTask<Void, Void, JSONObject>() {
        	Exception error;
    		@Override
    		protected JSONObject doInBackground(Void... voidParams) {
    	        String apiUrl = getApiUrl(tumblrName, "/post/delete");
    	    	
                Map<String, String> params = new HashMap<String, String>();
    	        params.put("id", id + "");

    	        try {
        	        JSONObject json = consumer.jsonFromPost(apiUrl, params);
        	        return json.getJSONObject("response");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(JSONObject response) {
				if (error == null) {
					callback.complete(response);
				} else {
					callback.failure(error);
				}
			}
        }.execute();
    }

    public void saveDraft(final String tumblrName, final long id, final Callback<JSONObject> callback) {
        new AsyncTask<Void, Void, JSONObject>() {
        	Exception error;
    		@Override
    		protected JSONObject doInBackground(Void... voidParams) {
    	        String apiUrl = getApiUrl(tumblrName, "/post/edit");
    	    	
                Map<String, String> params = new HashMap<String, String>();
    	        params.put("id", id + "");
    	        params.put("state", "draft");

    	        try {
        	        JSONObject json = consumer.jsonFromPost(apiUrl, params);
        	        return json.getJSONObject("response");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(JSONObject response) {
				if (error == null) {
					callback.complete(response);
				} else {
					callback.failure(error);
				}
			}
        }.execute();
    }

    public void schedulePost(final String tumblrName, final long id, final long timestamp, final Callback<JSONObject> callback) {
        new AsyncTask<Void, Void, JSONObject>() {
        	Exception error;
    		@Override
    		protected JSONObject doInBackground(Void... voidParams) {
    	        String apiUrl = getApiUrl(tumblrName, "/post/edit");
    	        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    	        String gmtDate = dateFormat.format(new Date(timestamp));
    	    	
                Map<String, String> params = new HashMap<String, String>();
    	        params.put("id", id + "");
    	        params.put("state", "queue");
    	        params.put("publish_on", gmtDate);

    	        try {
        	        JSONObject json = consumer.jsonFromPost(apiUrl, params);
        	        return json.getJSONObject("response");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(JSONObject response) {
				if (error == null) {
					callback.complete(response);
				} else {
					callback.failure(error);
				}
			}
        }.execute();
    }

    public void editPost(final String tumblrName, final Map<String, String> params, final Callback<JSONObject> callback) {
    	if (!params.containsKey("id")) {
    		callback.failure(new TumblrException("The id is mandatory to edit post"));
    		return;
    	}
        new AsyncTask<Void, Void, JSONObject>() {
        	Exception error;
    		@Override
    		protected JSONObject doInBackground(Void... voidParams) {
    	        String apiUrl = getApiUrl(tumblrName, "/post/edit");
    	    	
    	        try {
        	        JSONObject json = consumer.jsonFromPost(apiUrl, params);
        	        return json.getJSONObject("response");
				} catch (Exception e) {
					error = e;
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(JSONObject response) {
				if (error == null) {
					callback.complete(response);
				} else {
					callback.failure(error);
				}
			}
        }.execute();
    }
    
    public TumblrPost getPublicPosts(final String tumblrName, Map<String, String> params) {
        String apiUrl = getApiUrl(tumblrName, "/posts");
        
        Map<String, String> modifiedParams = new HashMap<String, String>(params);
        modifiedParams.put("base-hostname", tumblrName + ".tumblr.com");
        modifiedParams.put("api_key", consumer.getConsumerKey());

        try {
            JSONObject json = consumer.jsonFromGet(apiUrl, modifiedParams);
            return build(json.getJSONObject("response").getJSONArray("posts").getJSONObject(0));
        } catch (Exception e) {
            throw new TumblrException(e);
        }
    }        
    
	public static TumblrPost build(JSONObject json) throws JSONException {
		String type = json.getString("type");
		
		if (type.equals("photo")) {
			return new TumblrPhotoPost(json);
		}
		throw new IllegalArgumentException("Unable to build post for type " + type);
	}

	public void readPublicPhotoPosts(String tumblrName, String tag, PostRetriever postRetriever) {
    	try {
	        String blogUrl = tumblrName + ".tumblr.com";

			String apiUrl = "http://api.tumblr.com/v2/blog/" + blogUrl + "/posts/photo?limit=" + MAX_POST_PER_REQUEST;
			apiUrl += "&api_key=" + consumer.getConsumerKey();
			if (tag != null && tag.trim().length() > 0) {
				apiUrl += "&tag=" + URLEncoder.encode(tag, "UTF-8");
			}
			postRetriever.setApiUrl(apiUrl);
			postRetriever.execute();
		} catch (UnsupportedEncodingException e) {
			throw new TumblrException(e);
		}
	}
}
