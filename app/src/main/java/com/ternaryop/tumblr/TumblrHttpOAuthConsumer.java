package com.ternaryop.tumblr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.ternaryop.photoshelf.R;
import com.ternaryop.utils.JSONUtils;
import org.json.JSONArray;
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

        accessToken = TokenPreference.from(context).getAccessToken();
    }

    public static boolean isLogged(Context context) {
        return TokenPreference.from(context).isAccessTokenValid();
    }
    
    public static void logout(Context context) {
        TokenPreference.from(context).clearAccessToken();
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
        TokenPreference.from(context).storeRequestToken(requestToken);
        String authorizationUrl = oAuthService.getAuthorizationUrl(requestToken);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationUrl));
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(intent);
    }

    private static void access(final Context context, final Uri uri, final AuthenticationCallback callback) {
        new AccessAsyncTask(uri, callback, TokenPreference.from(context)).execute();
    }

    public static void loginWithActivity(final Context context) {
        new LoginAsyncTask().execute(context);
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
    
    public Response getSignedPostResponse(String url, Map<String, ?> params) throws IOException {
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

    public Response getSignedGetResponse(String url, Map<String, ?> params) {
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
            return checkResult(JSONUtils.jsonFromInputStream(getSignedGetResponse(url, params).getStream()));
        } catch (Exception e) {
            throw new TumblrException(e);
        }
    }

    public JSONObject jsonFromPost(String url, Map<String, ?> params) {
        try {
            return checkResult(JSONUtils.jsonFromInputStream(getSignedPostResponse(url, params).getStream()));
        } catch (Exception e) {
            throw new TumblrException(e);
        }
    }

    /**
     * Do not involve signed oAuth call, this is used to make public tumblr API requests
     * @param url the public url
     * @param params query parameters
     * @return the json
     */
    public JSONObject publicJsonFromGet(final String url, final Map<String, ?> params) {
        try {
            StringBuilder sbUrl = new StringBuilder(url + "?api_key=" + getConsumerKey());
            for (Map.Entry<String, ?> e : params.entrySet()) {
                sbUrl.append("&").append(e.getKey()).append("=").append(e.getValue());
            }
            return checkResult(JSONUtils.jsonFromUrl(sbUrl.toString()));
        } catch (Exception e) {
            throw new TumblrException(e);
        }
    }

    private JSONObject checkResult(JSONObject json) throws JSONException {
        if (!json.has("meta")) {
            throw new TumblrException("Invalid tumblr response, meta not found");
        }
        int status = json.getJSONObject("meta").getInt("status");
        if (status != 200 && status != 201) {
            String errorMessage = getErrorFromResponse(json);
            if (errorMessage == null) {
                errorMessage = json.getJSONObject("meta").getString("msg");
            }
            throw new TumblrException(errorMessage);
        }
        return json;
    }

    private String getErrorFromResponse(JSONObject json) throws JSONException {
        if (json.has("response")) {
            final JSONArray array = json.optJSONArray("response");
            // for example when an invalid id is passed the returned response contains an empty array
            if (array != null && array.length() == 0) {
                return null;
            }
            JSONObject response = json.getJSONObject("response");
            if (response.has("errors")) {
                JSONArray errors = response.getJSONArray("errors");
                ArrayList<String> list = new ArrayList<>();
                for (int i = 0; i < errors.length(); i++) {
                    list.add(errors.getString(i));
                }
                return TextUtils.join(",", list);
            }
        }
        return null;
    }

    private static class AccessAsyncTask extends AsyncTask<Void, Void, Token> {
        private final Uri uri;
        private AuthenticationCallback callback;
        private final TokenPreference prefs;
        private Exception error;

        public AccessAsyncTask(Uri uri, final AuthenticationCallback callback, TokenPreference prefs) {
            this.uri = uri;
            this.callback = callback;
            this.prefs = prefs;
        }

        @Override
        protected Token doInBackground(Void... params) {
            Context context = prefs.getContext();
            try {
                OAuthService oAuthService = new ServiceBuilder()
                        .provider(TumblrApi.class)
                        .apiKey(context.getString(R.string.CONSUMER_KEY))
                        .apiSecret(context.getString(R.string.CONSUMER_SECRET))
                        .callback(context.getString(R.string.CALLBACK_URL))
                        .build();
                return oAuthService.getAccessToken(prefs.getRequestToken(),
                        new Verifier(uri.getQueryParameter(OAuthConstants.VERIFIER)));
            } catch (Exception e) {
                error = e;
            }
            return null;
        }

        protected void onPostExecute(Token token) {
            if (token != null && error == null) {
                prefs.storeAccessToken(token);
            }
            if (callback != null) {
                if (token == null) {
                    callback.tumblrAuthenticated(null, null, error);
                } else {
                    callback.tumblrAuthenticated(token.getToken(), token.getSecret(), error);
                }
            }
        }
    }

    private static class LoginAsyncTask extends AsyncTask<Context, Void, Void> {
        @Override
        protected Void doInBackground(Context... params) {
            TumblrHttpOAuthConsumer.authorize(params[0]);
            return null;
        }
    }

    private static class TokenPreference {
        private static final String PREFS_NAME = "tumblr";
        private static final String PREF_OAUTH_SECRET = "oAuthSecret";
        private static final String PREF_OAUTH_TOKEN = "oAuthToken";

        private Context context;

        public TokenPreference(Context context) {
            this.context = context;
        }

        public static TokenPreference from(Context context) {
            return new TokenPreference(context);
        }

        public Context getContext() {
            return context;
        }

        public Token getRequestToken() {
            return getToken(context.getSharedPreferences(PREFS_NAME, 0));
        }

        public void storeRequestToken(Token requestToken) {
            storeToken(context.getSharedPreferences(PREFS_NAME, 0), requestToken);
        }

        public Token getAccessToken() {
            return getToken(PreferenceManager.getDefaultSharedPreferences(context));
        }

        public void storeAccessToken(Token accessToken) {
            storeToken(PreferenceManager.getDefaultSharedPreferences(context), accessToken);
        }

        public boolean isAccessTokenValid() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            return preferences.contains(PREF_OAUTH_TOKEN) && preferences.contains(PREF_OAUTH_SECRET);
        }

        public void clearAccessToken() {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            Editor edit = preferences.edit();
            edit.remove(PREF_OAUTH_TOKEN);
            edit.remove(PREF_OAUTH_SECRET);
            edit.apply();
        }

        private static Token getToken(SharedPreferences sharedPreferences) {
            return new Token(
                    sharedPreferences.getString(PREF_OAUTH_TOKEN, null),
                    sharedPreferences.getString(PREF_OAUTH_SECRET, null));
        }

        private static void storeToken(SharedPreferences sharedPreferences, Token token) {
            sharedPreferences
                    .edit()
                    .putString(PREF_OAUTH_TOKEN, token.getToken())
                    .putString(PREF_OAUTH_SECRET, token.getSecret())
                    .apply();
        }
    }
}
