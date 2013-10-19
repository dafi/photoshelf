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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.ternaryop.phototumblrshare.R;
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

    public void authorize(Context context, String consumerKey, String consumerSecret) {
    	provider = new CommonsHttpOAuthProvider(
		        REQUEST_TOKEN_URL,
		        ACCESS_TOKEN_URL,
		        AUTH_URL);

		// http://stackoverflow.com/questions/7841936/android-tumblr-oauth-signpost-401
		String authUrl;
		try {
		    // Callback url scheme is defined into manifest
		    authUrl = provider.retrieveRequestToken(this, context.getString(R.string.CALLBACK_URL));
		    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
		    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			context.startActivity(intent);
		} catch (OAuthException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	}

    public void access(final Context context, final String token, final String verifier, final AuthenticationCallback callback) {
		new AsyncTask<Void, Void, Exception>() {
			@Override
			protected Exception doInBackground(Void... params) {
				try {
					provider.retrieveAccessToken(TumblrHttpOAuthConsumer.this, verifier);
				} catch (OAuthException e) {
					return e;
				}
				return null;
			}
			protected void onPostExecute(Exception error) {
				Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
				edit.putString(PREF_OAUTH_TOKEN,  getToken());
				edit.putString(PREF_OAUTH_SECRET, getTokenSecret());
				edit.commit();
				if (callback != null) {
					callback.authenticated(getToken(), getTokenSecret(), error);
				}
			}
		}.execute();
	}

	public static void loginWithActivity(final Context context) {
		if (loginTumblr == null) {
			loginTumblr = new TumblrHttpOAuthConsumer(
					context.getString(R.string.CONSUMER_KEY),
					context.getString(R.string.CONSUMER_SECRET));
		}
        new AsyncTask<Void, Void, Void>() {
    		@Override
    		protected Void doInBackground(Void... params) {
    			loginTumblr.authorize(context, loginTumblr.getConsumerKey(), loginTumblr.getConsumerSecret());
    			return null;
    		}
        }.execute();
	}
	
	/**
	 * Return true if the uri scheme can be handled, false otherwise
	 * The returned value indicated only the scheme can be handled, the method complete the access asynchronously
	 * @param context
	 * @param uri
	 * @param callback can be null
	 * @return
	 */
	public static boolean handleOpenURI(final Context context, final Uri uri, AuthenticationCallback callback) {
		String callbackUrl = context.getString(R.string.CALLBACK_URL);
        boolean canHandleURI = uri != null && callbackUrl.startsWith(uri.getScheme());

        if (canHandleURI) {
        	loginTumblr.access(
        			context,
        			uri.getQueryParameter("oauth_token"),
        			uri.getQueryParameter("oauth_verifier"),
        			callback);
        }
        
        return canHandleURI;
	}
	
	public synchronized HttpResponse getSignedPostResponse(String url, List<NameValuePair> params) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException, ClientProtocolException, IOException {
        HttpPost request = new HttpPost(url);

        request.setEntity(new UrlEncodedFormEntity(params));
        CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(getConsumerKey(), getConsumerSecret());
        consumer.setTokenWithSecret(oAuthAccessKey, oAuthAccessSecret);
        consumer.sign(request);

        return new DefaultHttpClient().execute(request, new BasicHttpContext());
	}

	public HttpResponse getSignedGetResponse(String url, List<NameValuePair> params) throws ClientProtocolException, IOException, OAuthException, IllegalStateException {
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
	
    public JSONObject jsonFromGet(String url) {
		return jsonFromGet(url, null);
    }    

    public JSONObject jsonFromGet(String url, List<NameValuePair> params) {
    	try {
    		JSONObject json = JSONUtils.jsonFromInputStream(getSignedGetResponse(url, params).getEntity().getContent());
    		checkResult(json);
    		return json;
		} catch (Exception e) {
			throw new TumblrException(e);
		}
    }    

    public JSONObject jsonFromPost(String url, List<NameValuePair> params) {
    	try {
        	JSONObject json = JSONUtils.jsonFromInputStream(getSignedPostResponse(url, params).getEntity().getContent());
    		checkResult(json);
    		return json;
		} catch (Exception e) {
			throw new TumblrException(e);
		}
    }

	private void checkResult(JSONObject json) throws JSONException {
		if (!json.has("meta")) {
			throw new JSONException("Invalid tumblr response, meta not found");
		}
		int status = json.getJSONObject("meta").getInt("status");
		if (status != 200 && status != 201) {
			throw new JSONException(json.getJSONObject("meta").getString("msg"));
		}
	}    

}
