package com.ternaryop.photoshelf.dropbox;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSdkVersion;
import com.dropbox.core.android.Auth;
import com.dropbox.core.android.AuthActivity;
import com.dropbox.core.v2.DbxClientV2;
import com.ternaryop.photoshelf.R;

/**
 * Created by dave on 25/04/15.
 * Wrap dropbox session to handle token storing and cleanup
 */
public class DropboxManager {
    private static final String DROPBOX_ACCESS_KEY_NAME = "dropboxAccessKey";
    private static final String DROPBOX_ACCESS_SECRET_NAME = "dropboxAccessSecretName";
    private static final String DROPBOX_ACCOUNT_PREFS_NAME = "dropboxAccount";
    private static DropboxManager instance;

    private final SharedPreferences preferences;

    private final String appKey;
    private DbxClientV2 dbxClientV2;

    public static final String Version = DbxSdkVersion.Version;

    public static DropboxManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DropboxManager.class) {
                if (instance == null) {
                    instance = new DropboxManager(context);
                }
            }
        }
        return instance;
    }

    public DbxClientV2 getClient() {
        String accessToken = preferences.getString(DROPBOX_ACCESS_SECRET_NAME, null);
        if (accessToken == null) {
            return null;
        }
        if (dbxClientV2 == null) {
            DbxRequestConfig config = DbxRequestConfig.newBuilder("photoshelf-android/1.0").build();
            dbxClientV2 = new DbxClientV2(config, accessToken);
        }
        return dbxClientV2;
    }

    private DropboxManager(final Context context) {
        appKey = context.getString(R.string.DROPBOX_APP_KEY);
        preferences = context.getSharedPreferences(DROPBOX_ACCOUNT_PREFS_NAME, 0);
    }

    public boolean isLinked() {
        return preferences.contains(DROPBOX_ACCESS_SECRET_NAME);
    }

    private void storeAuth() {
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = Auth.getOAuth2Token();
        if (oauth2AccessToken != null) {
            SharedPreferences.Editor edit = preferences.edit();
            edit.putString(DROPBOX_ACCESS_KEY_NAME, "oauth2:");
            edit.putString(DROPBOX_ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.apply();
        }
    }

    private void clearKeys() {
        SharedPreferences.Editor edit = preferences.edit();
        edit.clear();
        edit.apply();
    }

    public String finishAuthentication() throws IllegalStateException {
        storeAuth();
        return preferences.getString(DROPBOX_ACCESS_SECRET_NAME, null);
    }

    public void unlink() {
        clearKeys();
    }

    public void startOAuth2AuthenticationForResult(Fragment fragment, int requestCode) {
        if (!AuthActivity.checkAppBeforeAuth(fragment.getActivity(), appKey, true /*alertUser*/)) {
            return;
        }

        Intent intent = AuthActivity.makeIntent(fragment.getActivity(), appKey, null, null);
        fragment.startActivityForResult(intent, requestCode);
    }
}
