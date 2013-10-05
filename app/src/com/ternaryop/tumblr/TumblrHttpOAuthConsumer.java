package com.ternaryop.tumblr;

import java.io.IOException;
import java.util.List;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.utils.DialogUtils;
import com.ternaryop.utils.JSONUtils;

public class TumblrHttpOAuthConsumer extends CommonsHttpOAuthConsumer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

    private static final String REQUEST_TOKEN_URL = "https://www.tumblr.com/oauth/request_token";
    private static final String ACCESS_TOKEN_URL = "https://www.tumblr.com/oauth/access_token";
    private static final String AUTH_URL = "https://www.tumblr.com/oauth/authorize";

    public static final String PREFS_NAME = "tumblr";
	public static final String PREF_OAUTH_SECRET = "oAuthSecret";
	public static final String PREF_OAUTH_TOKEN = "oAuthToken";

	private String oAuthAccessKey;
	private String oAuthAccessSecret;

	// provider is used only during login phases
	private CommonsHttpOAuthProvider provider;

	// Until the login isn't completed the instance tumblr must be null
	// When login is completed the loginTumblr isn't longer useful and tumblr can be set
	private static TumblrHttpOAuthConsumer loginTumblr; 

	public TumblrHttpOAuthConsumer(String consumerKey, String consumerSecret) {
		super(consumerKey, consumerSecret);
	}

	public TumblrHttpOAuthConsumer(final String consumerKey, final String consumerSecret, String oAuthAccessKey, String oAuthAccessSecret) {
		super(consumerKey, consumerSecret);
		this.oAuthAccessKey = oAuthAccessKey;
		this.oAuthAccessSecret = oAuthAccessSecret;
	}

	public static void login(Activity activity, Callback<Void> callback) {
		SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
		String authToken = settings.getString(PREF_OAUTH_TOKEN, null);
		String authTokenSecret = settings.getString(PREF_OAUTH_SECRET, null);
		
		if (authToken != null && authTokenSecret != null) {
			TumblrHttpOAuthConsumer consumer = new TumblrHttpOAuthConsumer(activity.getString(R.string.CONSUMER_KEY),
					activity.getString(R.string.CONSUMER_SECRET),
					authToken,
					authTokenSecret);
			callback.complete(new Tumblr(consumer), null);
		} else {
			if (loginTumblr == null) {
				loginTumblr = new TumblrHttpOAuthConsumer(activity.getString(R.string.CONSUMER_KEY),
						activity.getString(R.string.CONSUMER_SECRET));
			}
	       loginWithActivity(activity, callback);
		}
	}
	
	public void login(final Activity activity) {
        new AsyncTask<Void, Void, Object>() {
    		@Override
    		protected Object doInBackground(Void... params) {
    			authorize(activity, getConsumerKey(), getConsumerSecret());
    			return null;
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
		    authUrl = provider.retrieveRequestToken(this, activity.getString(R.string.CALLBACK_URL));
		    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
		    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			activity.startActivity(intent);
		} catch (OAuthException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	}

    public void access(final String token, final String verifier, final Callback<Void> callback) {
    	final TumblrHttpOAuthConsumer consumer = this;
		new AsyncTask<Void, Void, Object>() {
			@Override
			protected Object doInBackground(Void... params) {
				try {
					provider.retrieveAccessToken(consumer, verifier);
					oAuthAccessKey = getToken();
					oAuthAccessSecret = getTokenSecret();
				} catch (OAuthException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Object result) {
				callback.complete(new Tumblr(consumer), null);
			}
			
		}.execute();
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
							edit.putString(PREF_OAUTH_TOKEN,  loginTumblr.oAuthAccessKey);
							edit.putString(PREF_OAUTH_SECRET, loginTumblr.oAuthAccessSecret);
							edit.commit();
							//no longer necessary
							loginTumblr = null;

							callback.complete(t, null);
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

	public synchronized HttpResponse getSignedPostResponse(String url, List<NameValuePair> params) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, IOException {
        HttpPost request = new HttpPost(url);

        request.setEntity(new UrlEncodedFormEntity(params));
        CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(getConsumerKey(), getConsumerSecret());
        consumer.setTokenWithSecret(oAuthAccessKey, oAuthAccessSecret);
        consumer.sign(request);

        return new DefaultHttpClient().execute(request, new BasicHttpContext());
	}

	public HttpResponse getSignedGetResponse(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, OAuthException, IllegalStateException, JSONException {
        if (params != null) {
        	if(!url.endsWith("?")) {
                url += "?";
        	}
        	String paramString = URLEncodedUtils.format(params, "UTF-8");
            url += paramString;        	
        }
        HttpContext context = new BasicHttpContext();
        HttpRequestBase request = new HttpGet(url);

        // to be thread safe create a new consumer
        CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(getConsumerKey(), getConsumerSecret());
        consumer.setTokenWithSecret(oAuthAccessKey, oAuthAccessSecret);
        consumer.sign(request);

        return new DefaultHttpClient().execute(request, context);
    }    
	
    public JSONObject jsonFromGet(String url) throws ClientProtocolException, IOException, OAuthException, IllegalStateException, JSONException {
		return JSONUtils.jsonFromInputStream(getSignedGetResponse(url, null).getEntity().getContent());
    }    

    public JSONObject jsonFromGet(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, OAuthException, IllegalStateException, JSONException {
		return JSONUtils.jsonFromInputStream(getSignedGetResponse(url, params).getEntity().getContent());
    }    

    public JSONObject jsonFromPost(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, OAuthException, IllegalStateException, JSONException {
		return JSONUtils.jsonFromInputStream(getSignedPostResponse(url, params).getEntity().getContent());
    }    

}
