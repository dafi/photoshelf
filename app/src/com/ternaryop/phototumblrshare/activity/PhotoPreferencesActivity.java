package com.ternaryop.phototumblrshare.activity;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.Importer;
import com.ternaryop.tumblr.Tumblr;

@SuppressWarnings("deprecation")
public class PhotoPreferencesActivity extends PreferenceActivity {
    private static final String KEY_TUMBLR_LOGIN = "tumblr_login";
	private static final String KEY_IMPORT_POSTS = "import_posts";

	public static final int MAIN_PREFERENCES_RESULT = 1;

    private Preference preferenceTumblrLogin;
	private Preference preferenceImportPosts;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_main);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceTumblrLogin = preferenceScreen.findPreference(KEY_TUMBLR_LOGIN);
        preferenceImportPosts = preferenceScreen.findPreference(KEY_IMPORT_POSTS);
        preferenceImportPosts.setSummary(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "tags.csv");
    }


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// clicked the actionbar
			// close and return to caller
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == preferenceTumblrLogin) {
			Tumblr.login(this);
            return true;
        } else if (preference == preferenceImportPosts) {
	        String importPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "tags.csv";
        	Importer.importPosts(this, importPath);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
    
    public static void startPreferencesActivityForResult(Activity caller) {
		Intent intent = new Intent(caller, PhotoPreferencesActivity.class);
		Bundle bundle = new Bundle();

		intent.putExtras(bundle);

		caller.startActivityForResult(intent, MAIN_PREFERENCES_RESULT);
    }
}
