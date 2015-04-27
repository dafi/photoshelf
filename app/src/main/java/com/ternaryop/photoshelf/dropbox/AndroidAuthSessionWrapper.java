package com.ternaryop.photoshelf.dropbox;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.ternaryop.photoshelf.R;

/**
 * Created by dave on 25/04/15.
 * Wrap dropbox session to handle token storing and cleanup
 */
public class AndroidAuthSessionWrapper extends AndroidAuthSession {
    private static final String DROPBOX_ACCESS_KEY_NAME = "dropboxAccessKey";
    private static final String DROPBOX_ACCESS_SECRET_NAME = "dropboxAccessSecretName";
    private static final String DROPBOX_ACCOUNT_PREFS_NAME = "dropboxAccount";

    private static DropboxAPI<AndroidAuthSession> instance;
    private final SharedPreferences preferences;

    public static DropboxAPI<AndroidAuthSession> getInstance(Context context) {
        if (instance == null) {
            synchronized (AndroidAuthSessionWrapper.class) {
                if (instance == null) {
                    instance = new DropboxAPI<AndroidAuthSession>(new AndroidAuthSessionWrapper(context));
                }
            }
        }
        return instance;
    }

    private AndroidAuthSessionWrapper(final Context context) {
        super(new AppKeyPair(
                context.getString(R.string.DROPBOX_APP_KEY),
                context.getString(R.string.DROPBOX_APP_SECRET)));
        preferences = context.getSharedPreferences(DROPBOX_ACCOUNT_PREFS_NAME, 0);
        loadAuth();
    }

    public void loadAuth() {
        String key = preferences.getString(DROPBOX_ACCESS_KEY_NAME, null);
        String secret = preferences.getString(DROPBOX_ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) {
            return;
        }

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    public void storeAuth() {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences.Editor edit = preferences.edit();
            edit.putString(DROPBOX_ACCESS_KEY_NAME, "oauth2:");
            edit.putString(DROPBOX_ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.apply();
            return;
        }
        // Store the OAuth 1 access token, if there is one.  This is only necessary if
        // you're still using OAuth 1.
        AccessTokenPair oauth1AccessToken = getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences.Editor edit = preferences.edit();
            edit.putString(DROPBOX_ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(DROPBOX_ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.apply();
        }
    }

    private void clearKeys() {
        SharedPreferences.Editor edit = preferences.edit();
        edit.clear();
        edit.apply();
    }

    @Override
    public String finishAuthentication() throws IllegalStateException {
        String uid = super.finishAuthentication();
        storeAuth();
        return uid;
    }

    @Override
    public void unlink() {
        super.unlink();
        clearKeys();
    }

    public void startOAuth2AuthenticationForResult(Fragment fragment, int requestCode) {
        AppKeyPair appKeyPair = getAppKeyPair();
        if (!AuthActivity.checkAppBeforeAuth(fragment.getActivity(), appKeyPair.key, true /*alertUser*/)) {
            return;
        }

        Intent intent = AuthActivity.makeOAuth2Intent(fragment.getActivity(), appKeyPair.key, null, null);
        fragment.startActivityForResult(intent, requestCode);
    }
}
