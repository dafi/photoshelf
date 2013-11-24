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

import com.ternaryop.phototumblrshare.AppSupport;
import com.ternaryop.phototumblrshare.R;
import com.ternaryop.phototumblrshare.db.Importer;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.IOUtils;

@SuppressWarnings("deprecation")
public class PhotoPreferencesActivity extends PreferenceActivity {
    private static final String CSV_FILE_NAME = "tags.csv";
    private static final String DOM_FILTERS_FILE_NAME = "domSelectors.json";
	private static final String BIRTHDAYS_FILE_NAME = "birthdays.csv";

	private static final String KEY_TUMBLR_LOGIN = "tumblr_login";
	private static final String KEY_IMPORT_POSTS_FROM_CSV = "import_posts_from_csv";
	private static final String KEY_EXPORT_POSTS_FROM_CSV = "export_posts_csv";
	private static final String KEY_IMPORT_POSTS_FROM_TUMBLR = "import_posts_from_tumblr";
	private static final String KEY_IMPORT_DOM_FILTERS = "import_dom_filters";
	private static final String KEY_IMPORT_BIRTHDAYS = "import_birthdays";
	private static final String KEY_EXPORT_BIRTHDAYS = "export_birthdays";
    private static final String KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA = "import_birthdays_from_wikipedia";
	
	
	public static final int MAIN_PREFERENCES_RESULT = 1;

    private Preference preferenceTumblrLogin;
	private Preference preferenceImportPostsFromCSV;
	private Preference preferenceExportPostsToCSV;
	private Preference preferenceImportPostsFromTumblr;
	private Preference preferenceImportDOMFilters;
	private Preference preferenceImportBirthdays;
	private Preference preferenceExportBirthdays;
    private Preference preferenceImportBirthdaysFromWikipedia;
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_main);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceTumblrLogin = preferenceScreen.findPreference(KEY_TUMBLR_LOGIN);
        
        String csvPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CSV_FILE_NAME;
        preferenceImportPostsFromCSV = preferenceScreen.findPreference(KEY_IMPORT_POSTS_FROM_CSV);
		preferenceImportPostsFromCSV.setSummary(csvPath);
		preferenceImportPostsFromCSV.setEnabled(new File(csvPath).exists());

        preferenceExportPostsToCSV = preferenceScreen.findPreference(KEY_EXPORT_POSTS_FROM_CSV);
        preferenceExportPostsToCSV.setSummary(csvPath);
		
        preferenceImportPostsFromTumblr = preferenceScreen.findPreference(KEY_IMPORT_POSTS_FROM_TUMBLR);
        
        String domFiltersPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + DOM_FILTERS_FILE_NAME;
        preferenceImportDOMFilters = preferenceScreen.findPreference(KEY_IMPORT_DOM_FILTERS);
		preferenceImportDOMFilters.setSummary(domFiltersPath);
		preferenceImportDOMFilters.setEnabled(new File(domFiltersPath).exists());

        String birthdaysPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + BIRTHDAYS_FILE_NAME;
        preferenceImportBirthdays = preferenceScreen.findPreference(KEY_IMPORT_BIRTHDAYS);
        preferenceImportBirthdays.setSummary(birthdaysPath);
        preferenceImportBirthdays.setEnabled(new File(birthdaysPath).exists());

        preferenceExportBirthdays = preferenceScreen.findPreference(KEY_EXPORT_BIRTHDAYS);
        preferenceExportBirthdays.setSummary(birthdaysPath);

        preferenceImportBirthdaysFromWikipedia = preferenceScreen.findPreference(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA);
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
        } else if (preference == preferenceImportPostsFromCSV) {
	        String importPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CSV_FILE_NAME;
        	Importer.importPostsFromCSV(this, importPath);
            return true;
        } else if (preference == preferenceImportPostsFromTumblr) {
			Importer.importFromTumblr(this, new AppSupport(this).getSelectedBlogName());
            return true;
        } else if (preference == preferenceImportDOMFilters) {
	        String importPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + DOM_FILTERS_FILE_NAME;
			Importer.importDOMFilters(this, importPath);
            return true;
        } else if (preference == preferenceImportBirthdays) {
	        String importPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + BIRTHDAYS_FILE_NAME;
			Importer.importBirtdays(this, importPath);
            return true;
        } else if (preference == preferenceExportPostsToCSV) {
	        String exportPath = IOUtils.generateUniqueFileName(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + CSV_FILE_NAME);
			Importer.exportPostsToCSV(this, exportPath);
            return true;
        } else if (preference == preferenceExportBirthdays) {
	        String exportPath = IOUtils.generateUniqueFileName(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + BIRTHDAYS_FILE_NAME);
			Importer.exportBirthdaysToCSV(this, exportPath);
            return true;
        } else if (preference == preferenceImportBirthdaysFromWikipedia) {
            Importer.importMissingBirthdaysFromWikipedia(this, new AppSupport(this).getSelectedBlogName());
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
