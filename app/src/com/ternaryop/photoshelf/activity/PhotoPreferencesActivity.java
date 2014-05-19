package com.ternaryop.photoshelf.activity;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import com.dropbox.sync.android.DbxAccountManager;
import com.fedorvlasov.lazylist.ImageLoader;
import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Importer;
import com.ternaryop.tumblr.Tumblr;

@SuppressWarnings("deprecation")
public class PhotoPreferencesActivity extends PreferenceActivity {
    private static final String TUMBLR_SERVICE_NAME = "Tumblr";
	private static final String DROPBOX_SERVICE_NAME = "Dropbox";

    private static final String KEY_TUMBLR_LOGIN = "tumblr_login";
    private static final String KEY_DROPBOX_LOGIN = "dropbox_login";
    private static final String KEY_IMPORT_POSTS_FROM_CSV = "import_posts_from_csv";
    private static final String KEY_EXPORT_POSTS_FROM_CSV = "export_posts_csv";
    private static final String KEY_IMPORT_POSTS_FROM_TUMBLR = "import_posts_from_tumblr";
    private static final String KEY_IMPORT_DOM_FILTERS = "import_dom_filters";
    private static final String KEY_IMPORT_BIRTHDAYS = "import_birthdays";
    private static final String KEY_EXPORT_BIRTHDAYS = "export_birthdays";
    private static final String KEY_EXPORT_MISSING_BIRTHDAYS = "export_missing_birthdays";
    private static final String KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA = "import_birthdays_from_wikipedia";
    private static final String KEY_SCHEDULE_TIME_SPAN = "schedule_time_span";
    private static final String KEY_CLEAR_IMAGE_CACHE = "clear_image_cache";
    private static final String KEY_VERSION = "version";
    
    public static final int MAIN_PREFERENCES_RESULT = 1;
	private static final int DROPBOX_RESULT = 2;

    private Preference preferenceTumblrLogin;
    private Preference preferenceImportPostsFromCSV;
    private Preference preferenceExportPostsToCSV;
    private Preference preferenceImportPostsFromTumblr;
    private Preference preferenceImportDOMFilters;
    private Preference preferenceImportBirthdays;
    private Preference preferenceExportBirthdays;
    private Preference preferenceImportBirthdaysFromWikipedia;
    private Preference preferenceScheduleTimeSpan;
    private Preference preferenceClearImageCache;
    private Preference preferenceExportMissingBirthdays;
	private Preference preferenceDropboxLogin;

    private AppSupport appSupport;
	private DbxAccountManager dropboxManager;
	private Importer importer;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_main);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(prefListener);
        appSupport = new AppSupport(this);
        dropboxManager = appSupport.getDbxAccountManager();

        getActionBar().setDisplayHomeAsUpEnabled(true);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceTumblrLogin = preferenceScreen.findPreference(KEY_TUMBLR_LOGIN);

        if (Tumblr.isLogged(this)) {
            preferenceTumblrLogin.setTitle(getString(R.string.logout_title, TUMBLR_SERVICE_NAME));
        } else {
            preferenceTumblrLogin.setTitle(getString(R.string.login_title, TUMBLR_SERVICE_NAME));
        }
        
        preferenceDropboxLogin = preferenceScreen.findPreference(KEY_DROPBOX_LOGIN);
        if (dropboxManager.hasLinkedAccount()) {
        	preferenceDropboxLogin.setTitle(getString(R.string.logout_title, DROPBOX_SERVICE_NAME));
        } else {
        	preferenceDropboxLogin.setTitle(getString(R.string.login_title, DROPBOX_SERVICE_NAME));
        }
        
        String csvPath = Importer.getPostsPath();
        preferenceImportPostsFromCSV = preferenceScreen.findPreference(KEY_IMPORT_POSTS_FROM_CSV);
        preferenceImportPostsFromCSV.setSummary(csvPath);
        preferenceImportPostsFromCSV.setEnabled(new File(csvPath).exists());

        preferenceExportPostsToCSV = preferenceScreen.findPreference(KEY_EXPORT_POSTS_FROM_CSV);
        preferenceExportPostsToCSV.setSummary(csvPath);
        
        preferenceImportPostsFromTumblr = preferenceScreen.findPreference(KEY_IMPORT_POSTS_FROM_TUMBLR);
        
        String domFiltersPath = Importer.getDOMFiltersPath();
        preferenceImportDOMFilters = preferenceScreen.findPreference(KEY_IMPORT_DOM_FILTERS);
        preferenceImportDOMFilters.setSummary(domFiltersPath);
        preferenceImportDOMFilters.setEnabled(new File(domFiltersPath).exists());

        String birthdaysPath = Importer.getBirthdaysPath();
        preferenceImportBirthdays = preferenceScreen.findPreference(KEY_IMPORT_BIRTHDAYS);
        preferenceImportBirthdays.setSummary(birthdaysPath);
        preferenceImportBirthdays.setEnabled(new File(birthdaysPath).exists());

        preferenceExportBirthdays = preferenceScreen.findPreference(KEY_EXPORT_BIRTHDAYS);
        preferenceExportBirthdays.setSummary(birthdaysPath);

        preferenceImportBirthdaysFromWikipedia = preferenceScreen.findPreference(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA);
        
        preferenceScheduleTimeSpan = preferenceScreen.findPreference(KEY_SCHEDULE_TIME_SPAN);
        prefListener.onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(this), KEY_SCHEDULE_TIME_SPAN);

        preferenceClearImageCache = preferenceScreen.findPreference(KEY_CLEAR_IMAGE_CACHE);

        preferenceExportMissingBirthdays = preferenceScreen.findPreference(KEY_EXPORT_MISSING_BIRTHDAYS);
        
        setupVersionInfo(preferenceScreen);
    }

    // Use instance field for listener
    // It will not be gc'd as long as this instance is kept referenced
    private OnSharedPreferenceChangeListener prefListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(KEY_SCHEDULE_TIME_SPAN)) {
                int hours = sharedPreferences.getInt(key, 0);
                preferenceScheduleTimeSpan.setSummary(getResources().getQuantityString(R.plurals.hour_title, hours, hours));
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_OK && requestCode == DROPBOX_RESULT) {
            if (dropboxManager.hasLinkedAccount()) {
            	preferenceDropboxLogin.setTitle(getString(R.string.logout_title, DROPBOX_SERVICE_NAME));
            } else {
            	preferenceDropboxLogin.setTitle(getString(R.string.login_title, DROPBOX_SERVICE_NAME));
            }
    	}
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
            if (Tumblr.isLogged(this)) {
                logout();        
            } else {
                Tumblr.login(this);
            }
            return true;
        } else if (preference == preferenceImportPostsFromCSV) {
            getImporter().importPostsFromCSV(Importer.getPostsPath());
            return true;
        } else {
            if (preference == preferenceImportPostsFromTumblr) {
                getImporter().importFromTumblr(appSupport.getSelectedBlogName());
                return true;
            } else if (preference == preferenceImportDOMFilters) {
                getImporter().importDOMFilters(Importer.getDOMFiltersPath());
                return true;
            } else if (preference == preferenceImportBirthdays) {
                getImporter().importBirthdays(Importer.getBirthdaysPath());
                return true;
            } else if (preference == preferenceExportPostsToCSV) {
                getImporter().exportPostsToCSV(Importer.getExportPath(Importer.getPostsPath()));
                return true;
            } else if (preference == preferenceExportBirthdays) {
                getImporter().exportBirthdaysToCSV(Importer.getExportPath(Importer.getBirthdaysPath()));
                return true;
            } else if (preference == preferenceImportBirthdaysFromWikipedia) {
                getImporter().importMissingBirthdaysFromWikipedia(appSupport.getSelectedBlogName());
                return true;
            } else if (preference == preferenceClearImageCache) {
                clearImageCache();
                return true;
            } else if (preference == preferenceExportMissingBirthdays) {
                getImporter().exportMissingBirthdaysToCSV(Importer.getExportPath(Importer.getMissingBirthdaysPath()),
                        appSupport.getSelectedBlogName());
                return true;
            } else if (preference == preferenceDropboxLogin) {
            	if (dropboxManager.hasLinkedAccount()) {
            		dropboxManager.unlink();
                	preferenceDropboxLogin.setTitle(getString(R.string.login_title, DROPBOX_SERVICE_NAME));
            	} else {
            		dropboxManager.startLink(this, DROPBOX_RESULT);
            	}
                return true;
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void logout() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    Tumblr.logout(getApplicationContext());
                    break;
                }
            }
        };
        
        new AlertDialog.Builder(this)
        .setMessage("Are you sure?")
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, null)
        .show();
    }
    
    private void clearImageCache() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    ImageLoader.clearImageCache(PhotoPreferencesActivity.this);
                    break;
                }
            }
        };
        new AlertDialog.Builder(this)
        .setMessage(R.string.clear_cache_confirm)
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, null)
        .show();        
    }

    public static void startPreferencesActivityForResult(Activity caller) {
        Intent intent = new Intent(caller, PhotoPreferencesActivity.class);
        Bundle bundle = new Bundle();

        intent.putExtras(bundle);

        caller.startActivityForResult(intent, MAIN_PREFERENCES_RESULT);
    }

    private void setupVersionInfo(PreferenceScreen preferenceScreen) {
        Preference preferenceVersion = preferenceScreen.findPreference(KEY_VERSION);
        preferenceVersion.setTitle(getString(R.string.version_title, getString(R.string.app_name)));
        String version;
        try {
            String versionName = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionName;
            int versionCode = getPackageManager().getPackageInfo(
                    getPackageName(), 0).versionCode;
            version = String.valueOf("v" + versionName + " build " + versionCode);
        } catch (Exception e) {
            version = "N/A";
        }
        preferenceVersion.setSummary(version);
    }

    public Importer getImporter() {
    	if (importer == null) {
    		importer = new Importer(this, dropboxManager);
    	}
    	return importer;
    }
}
