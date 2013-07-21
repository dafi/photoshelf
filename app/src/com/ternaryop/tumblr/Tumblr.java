package com.ternaryop.tumblr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.utils.DialogUtils;

public class Tumblr {
	private static Tumblr tumblr;
	// Until the login isn't completed the instance tumblr must be null
	// When login is completed the loginTumblr isn't longer useful and tumblr can be set
	private static Tumblr loginTumblr; 

    private static final String REQUEST_TOKEN_URL = "https://www.tumblr.com/oauth/request_token";
    private static final String ACCESS_TOKEN_URL = "https://www.tumblr.com/oauth/access_token";
    private static final String AUTH_URL = "https://www.tumblr.com/oauth/authorize";

    public static final int POST_PER_REQUEST = 50;

    public static final String PREFS_NAME = "tumblr";
	public static final String PREF_OAUTH_SECRET = "oAuthSecret";
	public static final String PREF_OAUTH_TOKEN = "oAuthToken";

	private final String consumerKey;
	private final String consumerSecret;
	private String oAuthAccessKey;
	private String oAuthAccessSecret;

	private CommonsHttpOAuthConsumer consumer;
	// provider is used only during login phases
	private CommonsHttpOAuthProvider provider;

	public Tumblr(final String consumerKey, final String consumerSecret) {
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
    	consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
	}

	public Tumblr(final String consumerKey, final String consumerSecret, String oAuthAccessKey, String oAuthAccessSecret) {
		this(consumerKey, consumerSecret);
		this.oAuthAccessKey = oAuthAccessKey;
		this.oAuthAccessSecret = oAuthAccessSecret;
	}

	public void login(final Activity activity) {
        new AsyncTask<Void, Void, Object>() {
    		@Override
    		protected Object doInBackground(Void... params) {
    			authorize(activity, consumerKey, consumerSecret);
    			return null;
    		}
        }.execute();
    }

    public void access(final String token, final String verifier, final Callback<Void> callback) {
    	final Tumblr t = this;
		new AsyncTask<Void, Void, Object>() {
			@Override
			protected Object doInBackground(Void... params) {
				try {
					provider.retrieveAccessToken(consumer, verifier);
					oAuthAccessKey = consumer.getToken();
					oAuthAccessSecret = consumer.getTokenSecret();
				} catch (OAuthException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object result) {
				callback.complete(t, null);
			}
			
		}.execute();
	}

    protected JSONObject apiCall(String url) throws ClientProtocolException, IOException, OAuthException, IllegalStateException, JSONException {
        HttpContext context = new BasicHttpContext();
        HttpRequestBase request = new HttpGet(url);

        consumer.setTokenWithSecret(oAuthAccessKey, oAuthAccessSecret);
        consumer.sign(request);

        HttpResponse result = new DefaultHttpClient().execute(request, context);
		return jsonFromInputStream(result.getEntity().getContent());
    }    

    public void getBlogList(final Callback<Blog[]> callback) {
        new AsyncTask<Void, Void, Blog[]>() {

			@Override
    		protected Blog[] doInBackground(Void... params) {
    	        String apiUrl = "http://api.tumblr.com/v2/user/info";

    	        try {
    	        	JSONObject json = apiCall(apiUrl);
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
				callback.complete(tumblr, blogs);
			}
        }.execute();
    }
    
    
    public void authorize(Activity activity, String consumerKey, String consumerSecret) {
    	provider = new CommonsHttpOAuthProvider(
		        REQUEST_TOKEN_URL,
		        ACCESS_TOKEN_URL,
		        AUTH_URL);

		// http://stackoverflow.com/questions/7841936/android-tumblr-oauth-signpost-401
		String authUrl;
		try {
		    // Callback url scheme is defined into manifest
		    authUrl = provider.retrieveRequestToken(consumer, activity.getString(R.string.CALLBACK_URL));
		    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
		    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			activity.startActivity(intent);
		} catch (OAuthException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	}

	public String getOAuthAccessKey() {
		return oAuthAccessKey;
	}

	public String getOAuthAccessSecret() {
		return oAuthAccessSecret;
	}
	
	public static void getTumblr(Activity activity, Callback<Void> callback) {
		if (tumblr != null) {
			callback.complete(tumblr, null);
			return;
		}
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		String authToken = settings.getString(PREF_OAUTH_TOKEN, null);
		String authTokenSecret = settings.getString(PREF_OAUTH_SECRET, null);
		
		if (authToken != null && authTokenSecret != null) {
			tumblr = new Tumblr(activity.getString(R.string.CONSUMER_KEY),
					activity.getString(R.string.CONSUMER_SECRET),
					authToken,
					authTokenSecret);
			callback.complete(tumblr, null);
		} else {
			if (loginTumblr == null) {
				loginTumblr = new Tumblr(activity.getString(R.string.CONSUMER_KEY),
						activity.getString(R.string.CONSUMER_SECRET));
			}
	       loginWithActivity(activity, callback);
		}
	}
	private static void loginWithActivity(final Activity activity, final Callback<Void> callback) {
        Uri uri = activity.getIntent().getData();
        if (uri != null) {
        	loginTumblr.access(uri.getQueryParameter("oauth_token"),
        			uri.getQueryParameter("oauth_verifier"),
        			new Callback<Void>() {
						@Override
						public void complete(Tumblr t, Void result) {
							Editor edit = activity.getSharedPreferences(PREFS_NAME, 0).edit();
							edit.putString(PREF_OAUTH_TOKEN, t.getOAuthAccessKey());
							edit.putString(PREF_OAUTH_SECRET, t.getOAuthAccessSecret());
							edit.commit();
							tumblr = t;
							loginTumblr = null;
							callback.complete(tumblr, null);
						}

						@Override
						public void failure(Tumblr tumblr, Exception ex) {
							DialogUtils.showErrorDialog(activity, ex);
						}
					});
        } else {
        	loginTumblr.login(activity);
        }
	}

	public static JSONObject jsonFromInputStream(InputStream is) throws JSONException, IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line;
        while ((line = in.readLine()) != null) {
        	sb.append(line);
        }
		return (JSONObject) new JSONTokener(sb.toString()).nextValue();
	}

    protected String get_api_url(String tumblrName, String suffix) {
        return "http://api.tumblr.com/v2/blog/" + tumblrName + ".tumblr.com" + suffix;
    }
    
    public void draftPhotoPost(final String tumblrName, final String url, final String caption, final String tags, final Callback<Long> callback) {
    	createPhotoPost(tumblrName, url, caption, tags, "draft", callback);
    }

    public void publishPhotoPost(final String tumblrName, final String url, final String caption, final String tags, final Callback<Long> callback) {
    	createPhotoPost(tumblrName, url, caption, tags, "published", callback);
    }

    protected void createPhotoPost(final String tumblrName, final String url, final String caption, final String tags, final String state, final Callback<Long> callback) {
        new AsyncTask<Void, Void, Long>() {
    		@Override
    		protected Long doInBackground(Void... params) {
    	        String apiUrl = get_api_url(tumblrName, "/post");

    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
    	        nameValuePairs.add(new BasicNameValuePair("state", state));
    	        nameValuePairs.add(new BasicNameValuePair("type", "photo"));
    	        nameValuePairs.add(new BasicNameValuePair("source", url));
    	        nameValuePairs.add(new BasicNameValuePair("caption", caption));
    	        nameValuePairs.add(new BasicNameValuePair("tags", tags));
    	        try {

    	        	JSONObject json = postApiCall(apiUrl, nameValuePairs);
        	        return json.getJSONObject("response").getLong("id");
				} catch (Exception e) {
					// TODO: handle exception
					Log.e("err", "list", e);
				}
    	        return null;
    		}

			@Override
			protected void onPostExecute(Long postId) {
				callback.complete(tumblr, postId);
			}
        }.execute();
    }

    protected JSONObject postApiCall(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, OAuthException, IllegalStateException, JSONException {
        HttpContext context = new BasicHttpContext();
        HttpPost request = new HttpPost(url);

        request.setEntity(new UrlEncodedFormEntity(params));
        
        consumer.setTokenWithSecret(oAuthAccessKey, oAuthAccessSecret);
        consumer.sign(request);

        HttpResponse result = new DefaultHttpClient().execute(request, context);
		return jsonFromInputStream(result.getEntity().getContent());
    }    
}
