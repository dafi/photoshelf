package com.ternaryop.photoshelf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.ternaryop.photoshelf.dropbox.AndroidAuthSessionWrapper;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;

public class AppSupport {
    private static final String SUBDIRECTORY_PICTURES = "TernaryOpPhotoShelf";
    private static final String LAST_BIRTHDAY_SHOW_TIME = "lastBirthdayShowTime";
    private static final String AUTOMATIC_EXPORT = "automatic_export";

    // Preferences keys
    public static final String PREF_SELECTED_BLOG = "selectedBlog";
    public static final String PREF_BLOG_NAMES = "blogNames";
    public static final String PREF_SCHEDULE_MINUTES_TIME_SPAN = "schedule_minutes_time_span";
    public static final String PREF_EXPORT_DAYS_PERIOD = "exportDaysPeriod";
    public static final String PREF_LAST_FOLLOWERS_UPDATE_TIME = "lastFollowersUpdateTime";

    private final Context context;
    private final SharedPreferences preferences;

    public AppSupport(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public String getSelectedBlogName() {
        return preferences.getString(PREF_SELECTED_BLOG, null);
    }
    
    public void setSelectedBlogName(String blogName) {
        preferences.edit().putString(PREF_SELECTED_BLOG, blogName).apply();
    }
    
    public List<String> getBlogList() {
        Set<String> blogSet = preferences.getStringSet(PREF_BLOG_NAMES, null);
        if (blogSet == null) {
            return null;
        }
        ArrayList<String> list = new ArrayList<>(blogSet);
        Collections.sort(list);
        return list;
    }
    
    public void setBlogList(List<String> blogNames) {
        setBlogList(new HashSet<>(blogNames));
    }

    public void setBlogList(Set<String> blogNames) {
        preferences.edit().putStringSet(PREF_BLOG_NAMES, blogNames).apply();
    }

    public int getDefaultScheduleMinutesTimeSpan() {
        return preferences.getInt(PREF_SCHEDULE_MINUTES_TIME_SPAN,
                context.getResources().getInteger(R.integer.schedule_minutes_time_span_default));
    }

    public Context getContext() {
        return context;
    }

    public void fetchBlogNames(final Context context, final AppSupportCallback callback) {
        List<String> blogList = getBlogList();
        if (blogList != null) {
            callback.onComplete(this, null);
            return;
        }
        Tumblr.getSharedTumblr(context).getBlogList(new Callback<Blog[]>() {

            @Override
            public void complete(Blog[] blogs) {
                HashSet<String> blogNames = new HashSet<>(blogs.length);
                String primaryBlog = null;
                for (Blog blog : blogs) {
                    blogNames.add(blog.getName());
                    if (blog.isPrimary()) {
                        primaryBlog = blog.getName();
                    }
                }
                setBlogList(blogNames);
                if (primaryBlog != null) {
                    setSelectedBlogName(primaryBlog);
                }
                if (callback != null) {
                    callback.onComplete(AppSupport.this, null);
                }
            }

            @Override
            public void failure(Exception e) {
                if (callback != null) {
                    callback.onComplete(AppSupport.this, e);
                }
            }
        });
    }
    
    public interface AppSupportCallback {
        void onComplete(AppSupport appSupport, Exception error);
    }
    
    public long getLastBirthdayShowTime() {
        return preferences.getLong(LAST_BIRTHDAY_SHOW_TIME, 0);
    }
    
    public void setLastBirthdayShowTime(long timems) {
        preferences.edit().putLong(LAST_BIRTHDAY_SHOW_TIME, timems).apply();
    }

    public static File getPicturesDirectory() {
        File fullDirPath = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), SUBDIRECTORY_PICTURES);
        if (!fullDirPath.exists()) {
            fullDirPath.mkdirs();
        }
        return fullDirPath;
    }

    public boolean isAutomaticExportEnabled() {
        return preferences.getBoolean(AUTOMATIC_EXPORT, false);
    }

    public DropboxAPI<AndroidAuthSession> getDbxAccountManager() {
        return AndroidAuthSessionWrapper.getInstance(context);
    }

    public int getExportDaysPeriod() {
        return preferences.getInt(PREF_EXPORT_DAYS_PERIOD,
                context.getResources().getInteger(R.integer.export_days_period_default));
    }

    public void setLastFollowersUpdateTime(long millisecs) {
        preferences.edit().putLong(PREF_LAST_FOLLOWERS_UPDATE_TIME, millisecs).apply();
    }

    public long getLastFollowersUpdateTime() {
        return preferences.getLong(PREF_LAST_FOLLOWERS_UPDATE_TIME, -1);
    }

}
