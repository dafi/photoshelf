package com.ternaryop.photoshelf.fragment;

import java.io.File;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import com.ternaryop.photoshelf.AppSupport;
import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Importer;
import com.ternaryop.photoshelf.dropbox.DropboxManager;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.utils.DateTimeUtils;

@SuppressWarnings("deprecation")
public class PhotoPreferencesFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    private static final String TUMBLR_SERVICE_NAME = "Tumblr";
    private static final String DROPBOX_SERVICE_NAME = "Dropbox";

    private static final String KEY_TUMBLR_LOGIN = "tumblr_login";
    private static final String KEY_DROPBOX_LOGIN = "dropbox_login";
    private static final String KEY_IMPORT_POSTS_FROM_CSV = "import_posts_from_csv";
    private static final String KEY_EXPORT_POSTS_FROM_CSV = "export_posts_csv";
    private static final String KEY_IMPORT_POSTS_FROM_TUMBLR = "import_posts_from_tumblr";
    private static final String KEY_IMPORT_DOM_FILTERS = "import_dom_filters";
    private static final String KEY_IMPORT_TITLE_PARSER = "import_title_parser";
    private static final String KEY_IMPORT_BIRTHDAYS = "import_birthdays";
    private static final String KEY_EXPORT_BIRTHDAYS = "export_birthdays";
    private static final String KEY_EXPORT_MISSING_BIRTHDAYS = "export_missing_birthdays";
    private static final String KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA = "import_birthdays_from_wikipedia";
    private static final String KEY_CLEAR_IMAGE_CACHE = "clear_image_cache";
    private static final String KEY_VERSION = "version";
    private static final String KEY_DROPBOX_VERSION = "dropbox_version";
    private static final String KEY_THUMBNAIL_WIDTH = "thumbnail_width";

    private static final int DROPBOX_RESULT = 2;

    private Preference preferenceTumblrLogin;
    private Preference preferenceImportPostsFromCSV;
    private Preference preferenceExportPostsToCSV;
    private Preference preferenceImportPostsFromTumblr;
    private Preference preferenceImportDOMFilters;
    private Preference preferenceImportTitleParser;
    private Preference preferenceImportBirthdays;
    private Preference preferenceExportBirthdays;
    private Preference preferenceImportBirthdaysFromWikipedia;
    private Preference preferenceScheduleTimeSpan;
    private Preference preferenceClearImageCache;
    private Preference preferenceExportMissingBirthdays;
    private Preference preferenceDropboxLogin;
    private ListPreference preferenceThumbnailWidth;
    private Preference preferenceExportDaysPeriod;

    private AppSupport appSupport;
    private DropboxManager dropboxManager;
    private Importer importer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_main);

        appSupport = new AppSupport(getActivity());
        dropboxManager = DropboxManager.getInstance(getActivity());

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceTumblrLogin = preferenceScreen.findPreference(KEY_TUMBLR_LOGIN);

        if (Tumblr.isLogged(getActivity())) {
            preferenceTumblrLogin.setTitle(getString(R.string.logout_title, TUMBLR_SERVICE_NAME));
        } else {
            preferenceTumblrLogin.setTitle(getString(R.string.login_title, TUMBLR_SERVICE_NAME));
        }
        
        preferenceDropboxLogin = preferenceScreen.findPreference(KEY_DROPBOX_LOGIN);
        if (dropboxManager.isLinked()) {
            preferenceDropboxLogin.setTitle(getString(R.string.logout_title, DROPBOX_SERVICE_NAME));
        } else {
            preferenceDropboxLogin.setTitle(getString(R.string.login_title, DROPBOX_SERVICE_NAME));
        }
        
        String csvPath = Importer.getPostsPath();
        preferenceImportPostsFromCSV = setupPreferenceFilePath(csvPath, KEY_IMPORT_POSTS_FROM_CSV, preferenceScreen);

        preferenceExportPostsToCSV = preferenceScreen.findPreference(KEY_EXPORT_POSTS_FROM_CSV);
        preferenceExportPostsToCSV.setSummary(csvPath);
        
        preferenceImportPostsFromTumblr = preferenceScreen.findPreference(KEY_IMPORT_POSTS_FROM_TUMBLR);
        
        preferenceImportDOMFilters = setupPreferenceFilePath(Importer.getDOMFiltersPath(), KEY_IMPORT_DOM_FILTERS, preferenceScreen);
        preferenceImportTitleParser = setupPreferenceFilePath(Importer.getTitleParserPath(), KEY_IMPORT_TITLE_PARSER, preferenceScreen);

        String birthdaysPath = Importer.getBirthdaysPath();
        preferenceImportBirthdays = setupPreferenceFilePath(birthdaysPath, KEY_IMPORT_BIRTHDAYS, preferenceScreen);

        preferenceExportBirthdays = preferenceScreen.findPreference(KEY_EXPORT_BIRTHDAYS);
        preferenceExportBirthdays.setSummary(birthdaysPath);

        preferenceImportBirthdaysFromWikipedia = preferenceScreen.findPreference(KEY_IMPORT_BIRTHDAYS_FROM_WIKIPEDIA);
        
        preferenceScheduleTimeSpan = preferenceScreen.findPreference(AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN);
        onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(getActivity()), AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN);

        preferenceClearImageCache = preferenceScreen.findPreference(KEY_CLEAR_IMAGE_CACHE);

        preferenceExportMissingBirthdays = preferenceScreen.findPreference(KEY_EXPORT_MISSING_BIRTHDAYS);

        preferenceThumbnailWidth = (ListPreference) preferenceScreen.findPreference(KEY_THUMBNAIL_WIDTH);
        onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(getActivity()), KEY_THUMBNAIL_WIDTH);

        preferenceExportDaysPeriod = preferenceScreen.findPreference(AppSupport.PREF_EXPORT_DAYS_PERIOD);
        onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(getActivity()), AppSupport.PREF_EXPORT_DAYS_PERIOD);

        setupVersionInfo(preferenceScreen);
    }

    private Preference setupPreferenceFilePath(String fullPath, String prefKey, PreferenceScreen preferenceScreen) {
        Preference pref = preferenceScreen.findPreference(prefKey);
        pref.setSummary(fullPath);
        pref.setEnabled(new File(fullPath).exists());
        return pref;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case AppSupport.PREF_SCHEDULE_MINUTES_TIME_SPAN:
                int minutes = sharedPreferences.getInt(key, 0);
                preferenceScheduleTimeSpan.setSummary(getResources().getQuantityString(R.plurals.minute_title, minutes, minutes));
                break;
            case KEY_THUMBNAIL_WIDTH:
                String value = sharedPreferences.getString(key, getResources().getString(R.string.thumbnail_width_value_default));
                int index = preferenceThumbnailWidth.findIndexOfValue(value);
                if (index > -1) {
                    preferenceThumbnailWidth.setSummary(preferenceThumbnailWidth.getEntries()[index]);
                }
                break;
            case AppSupport.PREF_EXPORT_DAYS_PERIOD:
                int days = sharedPreferences.getInt(key, appSupport.getExportDaysPeriod());
                long lastFollowersUpdateTime = appSupport.getLastFollowersUpdateTime();
                String remainingMessage;
                if (lastFollowersUpdateTime < 0) {
                    remainingMessage = getResources().getString(R.string.never_run);
                } else {
                    int remainingDays = (int) (days - DateTimeUtils.daysSinceTimestamp(lastFollowersUpdateTime));
                    remainingMessage = getResources().getQuantityString(R.plurals.next_in_day, remainingDays, remainingDays);
                }
                preferenceExportDaysPeriod.setSummary(getResources().getQuantityString(R.plurals.day_title, days, days)
                        + " (" + remainingMessage + ")");
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DROPBOX_RESULT) {
            boolean authenticationSuccessful = dropboxManager.finishAuthentication() != null;
            if (authenticationSuccessful) {
                preferenceDropboxLogin.setTitle(getString(R.string.logout_title, DROPBOX_SERVICE_NAME));
            } else {
                preferenceDropboxLogin.setTitle(getString(R.string.login_title, DROPBOX_SERVICE_NAME));
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @NonNull Preference preference) {
        if (preference == preferenceTumblrLogin) {
            if (Tumblr.isLogged(getActivity())) {
                logout();        
            } else {
                Tumblr.login(getActivity());
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
                getImporter().importFile(Importer.getDOMFiltersPath(), Importer.DOM_FILTERS_FILE_NAME);
                return true;
            } else if (preference == preferenceImportTitleParser) {
                getImporter().importFile(Importer.getDOMFiltersPath(), Importer.TITLE_PARSER_FILE_NAME);
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
                if (dropboxManager.isLinked()) {
                    dropboxManager.unlink();
                    preferenceDropboxLogin.setTitle(getString(R.string.login_title, DROPBOX_SERVICE_NAME));
                } else {
                    DropboxManager.getInstance(getActivity()).startOAuth2AuthenticationForResult(this, DROPBOX_RESULT);
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
                    Tumblr.logout(getActivity().getApplicationContext());
                    break;
                }
            }
        };
        
        new AlertDialog.Builder(getActivity())
        .setMessage(getString(R.string.are_you_sure))
        .setPositiveButton(android.R.string.yes, dialogClickListener)
        .setNegativeButton(android.R.string.no, null)
        .show();
    }
    
    private void clearImageCache() {
        // copied from https://github.com/UweTrottmann/SeriesGuide/
        // try to open app info where user can clear app cache folders
        Intent intent = new Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // open all apps view
            intent = new Intent(
                    android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            startActivity(intent);
        }
    }

    private void setupVersionInfo(PreferenceScreen preferenceScreen) {
        Preference preferenceVersion = preferenceScreen.findPreference(KEY_VERSION);
        preferenceVersion.setTitle(getString(R.string.version_title, getString(R.string.app_name)));
        String version;
        try {
            String versionName = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), 0).versionName;
            int versionCode = getActivity().getPackageManager().getPackageInfo(
                    getActivity().getPackageName(), 0).versionCode;
            version = String.valueOf(versionName + " build " + versionCode);
        } catch (Exception e) {
            version = "N/A";
        }
        preferenceVersion.setSummary(version);

        // dropbox
        preferenceVersion = preferenceScreen.findPreference(KEY_DROPBOX_VERSION);
        preferenceVersion.setTitle(getString(R.string.version_title, "Dropbox Core"));
        preferenceVersion.setSummary(DropboxManager.Version);
    }

    public Importer getImporter() {
        if (importer == null) {
            importer = new Importer(getActivity(), dropboxManager);
        }
        return importer;
    }

    public ActionBar getSupportActionBar() {
        return ((AppCompatActivity)getActivity()).getSupportActionBar();
    }
}
