package com.ternaryop.tumblr;

import java.io.IOException;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.ternaryop.photoshelf.R;
import com.ternaryop.utils.JSONUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TumblrApi;
import org.scribe.model.OAuthConstants;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

public class TumblrHttpOAuthConsumer {
    private static final String PREFS_NAME = "tumblr";
    private static final String PREF_OAUTH_SECRET = "oAuthSecret";
    private static final String PREF_OAUTH_TOKEN = "oAuthToken";

    private final OAuthService oAuthService;
    private final Token accessToken;
    private final String consumerKey;

    public TumblrHttpOAuthConsumer(Context context) {
        consumerKey = context.getString(R.string.CONSUMER_KEY);

        oAuthService = new ServiceBuilder()
        .provider(TumblrApi.class)
        .apiKey(consumerKey)
        .apiSecret(context.getString(R.string.CONSUMER_SECRET))
        .callback(context.getString(R.string.CALLBACK_URL))
        .build();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        accessToken = new Token(
                preferences.getString(PREF_OAUTH_TOKEN, null),
                preferences.getString(PREF_OAUTH_SECRET, null));
    }

    public static boolean isLogged(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.contains(PREF_OAUTH_TOKEN) && preferences.contains(PREF_OAUTH_SECRET);
    }
    
    public static void logout(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = preferences.edit();
        edit.remove(PREF_OAUTH_TOKEN);
        edit.remove(PREF_OAUTH_SECRET);
        edit.apply();
    }
    
    public String getConsumerKey() {
        return consumerKey;
    }
    
    private static void authorize(Context context) {
        // Callback url scheme is defined into manifest
        OAuthService oAuthService = new ServiceBuilder()
        .provider(TumblrApi.class)
        .apiKey(context.getString(R.string.CONSUMER_KEY))
        .apiSecret(context.getString(R.string.CONSUMER_SECRET))
        .callback(context.getString(R.string.CALLBACK_URL))
        .build();
        Token requestToken = oAuthService.getRequestToken();
        Editor edit = context.getSharedPreferences(PREFS_NAME, 0).edit();
        edit.putString(PREF_OAUTH_TOKEN, requestToken.getToken());
        edit.putString(PREF_OAUTH_SECRET, requestToken.getSecret());
        edit.apply();
        String authorizationUrl = oAuthService.getAuthorizationUrl(requestToken);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl));
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    private static void access(final Context context, final Uri uri, final AuthenticationCallback callback) {
        new AsyncTask<Void, Void, Token>() {
            Exception error;
            @Override
            protected Token doInBackground(Void... params) {
                Token accessToken = null;
                try {
                    OAuthService oAuthService = new ServiceBuilder()
                    .provider(TumblrApi.class)
                    .apiKey(context.getString(R.string.CONSUMER_KEY))
                    .apiSecret(context.getString(R.string.CONSUMER_SECRET))
                    .callback(context.getString(R.string.CALLBACK_URL))
                    .build();
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
                    Token requestToken = new Token(
                            prefs.getString(PREF_OAUTH_TOKEN, ""),
                            prefs.getString(PREF_OAUTH_SECRET, ""));
                    accessToken = oAuthService.getAccessToken(requestToken,
                                    new Verifier(uri.getQueryParameter(OAuthConstants.VERIFIER)));
                } catch (Exception e) {
                    error = e;
                }
                return accessToken;
            }
            protected void onPostExecute(Token token) {
                if (token != null && error == null) {
                    Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    edit.putString(PREF_OAUTH_TOKEN,  token.getToken());
                    edit.putString(PREF_OAUTH_SECRET, token.getSecret());
                    edit.apply();
                }
                if (callback != null) {
                    if (token == null) {
                        callback.tumblrAuthenticated(null, null, error);
                    } else {
                        callback.tumblrAuthenticated(token.getToken(), token.getSecret(), error);
                    }
                }
            }
        }.execute();
    }

    public static void loginWithActivity(final Context context) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                TumblrHttpOAuthConsumer.authorize(context);
                return null;
            }
        }.execute();
    }
    
    /**
     * Return true if the uri scheme can be handled, false otherwise
     * The returned value indicated only the scheme can be handled, the method complete the access asynchronously
     * @param context the context
     * @param uri the uri to check
     * @param callback can be null
     * @return true if uri can be handled, false otherwise
     */
    public static boolean handleOpenURI(final Context context, final Uri uri, AuthenticationCallback callback) {
        String callbackUrl = context.getString(R.string.CALLBACK_URL);
        boolean canHandleURI = uri != null && callbackUrl.startsWith(uri.getScheme());

        if (canHandleURI) {
            TumblrHttpOAuthConsumer.access(
                    context,
                    uri,
                    callback);
        }
        
        return canHandleURI;
    }
    
    public synchronized Response getSignedPostResponse(String url, Map<String, ?> params) throws IOException {
        OAuthRequest oAuthReq = new OAuthRequest(Verb.POST, url);

        for (String key : params.keySet()) {
            Object value = params.get(key);
            if (value instanceof String) {
                oAuthReq.addBodyParameter(key, (String)value);
            }
        }
        oAuthService.signRequest(accessToken, oAuthReq);
        return new MultipartConverter(oAuthReq, params).getRequest().send();
    }

    public synchronized Response getSignedGetResponse(String url, Map<String, ?> params) {
        OAuthRequest oAuthReq = new OAuthRequest(Verb.GET, url);

        if (params != null) {
            for (String key : params.keySet()) {
                Object value = params.get(key);
                oAuthReq.addQuerystringParameter(key, value.toString());
            }

        }
        oAuthService.signRequest(accessToken, oAuthReq);
        return oAuthReq.send();
    }
    
    public JSONObject jsonFromGet(String url) {
        return jsonFromGet(url, null);
    }    

    public JSONObject jsonFromGet(String url, Map<String, ?> params) {
        try {
            JSONObject json = JSONUtils.jsonFromInputStream(getSignedGetResponse(url, params).getStream());
            checkResult(json);
            return json;
        } catch (Exception e) {
            throw new TumblrException(e);
        }
    }    

    public JSONObject jsonFromPost(String url, Map<String, ?> params) {
        try {
            JSONObject json = JSONUtils.jsonFromInputStream(getSignedPostResponse(url, params).getStream());
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
