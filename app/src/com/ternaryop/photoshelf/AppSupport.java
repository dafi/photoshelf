package com.ternaryop.photoshelf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.dropbox.sync.android.DbxAccountManager;
import com.ternaryop.tumblr.Blog;
import com.ternaryop.tumblr.Callback;
import com.ternaryop.tumblr.Tumblr;

public class AppSupport {
    private static final String SUBDIRECTORY_PICTURES = "TernaryOpPhotoShelf";

    private final Context context;
	private static final String PREF_SELECTED_BLOG = "selectedBlog";
	private static final String PREF_BLOG_NAMES = "blogNames";
	private static final String PREF_SCHEDULE_TIME_SPAN = "schedule_time_span";
	private static final String LAST_BIRTHDAY_SHOW_TIME = "lastBirthdayShowTime";
    private static final String AUTOMATIC_EXPORT = "automatic_export";
	private SharedPreferences preferences;

	public AppSupport(Context context) {
		this.context = context;
		preferences = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	public String getSelectedBlogName() {
		return preferences.getString(PREF_SELECTED_BLOG, null);
	}
	
	public void setSelectedBlogName(String blogName) {
		Editor edit = preferences.edit();
		edit.putString(PREF_SELECTED_BLOG, blogName);
		edit.commit();
	}
	
	public List<String> getBlogList() {
		Set<String> blogSet = preferences.getStringSet(PREF_BLOG_NAMES, null);
		if (blogSet == null) {
			return null;
		}
		ArrayList<String> list = new ArrayList<String>(blogSet);
		Collections.sort(list);
		return list;
	}
	
	public void setBlogList(List<String> blogNames) {
		setBlogList(new HashSet<String>(blogNames));
	}

	public void setBlogList(Set<String> blogNames) {
		Editor edit = preferences.edit();
		edit.putStringSet(PREF_BLOG_NAMES, blogNames);
		edit.commit();
	}

	public int getDefaultScheduleHoursSpan() {
		return preferences.getInt(PREF_SCHEDULE_TIME_SPAN,
				context.getResources().getInteger(R.integer.schedule_time_span_default));
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
				HashSet<String> blogNames = new HashSet<String>(blogs.length);
				String primaryBlog = null;
				for (int i = 0; i < blogs.length; i++) {
					blogNames.add(blogs[i].getName());
					if (blogs[i].isPrimary()) {
						primaryBlog = blogs[i].getName();
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
		public void onComplete(AppSupport appSupport, Exception error);
	}
	
	public long getLastBirthdayShowTime() {
		return preferences.getLong(LAST_BIRTHDAY_SHOW_TIME, 0);
	}
	
	public void setLastBirthdayShowTime(long timems) {
		Editor editor = preferences.edit();
		editor.putLong(LAST_BIRTHDAY_SHOW_TIME, timems);
		editor.commit();
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

    public DbxAccountManager getDbxAccountManager() {
        return DbxAccountManager.getInstance(context.getApplicationContext(),
                context.getString(R.string.DROPBOX_APP_KEY),
                context.getString(R.string.DROPBOX_APP_SECRET));

    }
}
