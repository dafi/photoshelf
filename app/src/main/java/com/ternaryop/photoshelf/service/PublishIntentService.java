package com.ternaryop.photoshelf.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.db.BirthdayDAO;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.util.NotificationUtil;
import com.ternaryop.photoshelf.util.log.Log;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;

/**
 * Created by dave on 01/03/14.
 * Contains all methods used to publish posts
 */
public class PublishIntentService extends IntentService {
    public static final String URL = "url";
    public static final String BLOG_NAME = "blogName";
    public static final String POST_TITLE = "postTitle";
    public static final String POST_TAGS = "postTags";
    public static final String ACTION = "action";
    public static final String PUBLISH_ACTION_DRAFT = "draft";
    public static final String PUBLISH_ACTION_PUBLISH = "publish";

    public static final String BIRTHDAY_LIST_BY_DATE_ACTION = "birthdayListByDate";
    public static final String BIRTHDAY_DATE = "birthDate";
    public static final String RESULT_LIST1 = "list1";

    // Intents returned using local broadcast
    public static final String BIRTHDAY_INTENT = "birthdayIntent";

    private NotificationUtil notificationUtil;

    public PublishIntentService() {
        super("publishIntent");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationUtil = new NotificationUtil(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Uri url = intent.getParcelableExtra(URL);
        String selectedBlogName = intent.getStringExtra(BLOG_NAME);
        String postTitle = intent.getStringExtra(POST_TITLE);
        String postTags = intent.getStringExtra(POST_TAGS);
        String action = intent.getStringExtra(ACTION);

        try {
            addBithdateFromTags(postTags, selectedBlogName);
            if (PUBLISH_ACTION_DRAFT.equals(action)) {
                Tumblr.getSharedTumblr(getApplicationContext()).draftPhotoPost(selectedBlogName,
                        url, postTitle, postTags);
            } else if (PUBLISH_ACTION_PUBLISH.equals(action)) {
                Tumblr.getSharedTumblr(getApplicationContext()).publishPhotoPost(selectedBlogName,
                        url, postTitle, postTags);
            } else if (BIRTHDAY_LIST_BY_DATE_ACTION.equals(action)) {
                broadcastBirthdaysByDate(intent);
            }
        } catch (Exception ex) {
            logError(intent, ex);
        }
    }

    private void addBithdateFromTags(final String postTags, final String blogName) {
        if (postTags == null) {
            return;
        }
        String[] tags = postTags.split(",");
        if (tags.length == 0) {
            return;
        }
        final String name = tags[0].trim();

        new Thread(new Runnable() {
            public void run() {
                BirthdayDAO birthdayDAO = DBHelper.getInstance(getApplicationContext()).getBirthdayDAO();
                SQLiteDatabase db = birthdayDAO.getDbHelper().getWritableDatabase();
                db.beginTransaction();

                try {
                    if (birthdayDAO.getBirthdayByName(name, blogName) != null) {
                        return;
                    }
                    Birthday birthday = BirthdayUtils.searchBirthday(getApplicationContext(), name, blogName);
                    if (birthday != null) {
                        birthdayDAO.insert(birthday);
                        db.setTransactionSuccessful();
                        notificationUtil.notifyBirthdayAdded(name, birthday.getBirthDate());
                    }
                } catch (Exception e) {
                    notificationUtil.notifyError(e, name, getString(R.string.birthday_add_error_ticker));
                } finally {
                    db.endTransaction();
                }
            }
        }).start();
    }

    private void logError(Intent intent, Exception e) {
        Uri url = intent.getParcelableExtra(URL);
        String postTags = intent.getStringExtra(POST_TAGS);

        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "publish_errors.txt");
            Log.error(e, file, " Error on url " + url, " tags " + postTags);
        } catch (Exception ignored) {
        }

        notificationUtil.notifyError(e, postTags, getString(R.string.upload_error_ticker), url.hashCode());
    }

    public static void startActionIntent(@NonNull Context context,
                                         @NonNull Uri url,
                                         @NonNull String blogName,
                                         @NonNull String postTitle,
                                         @NonNull String postTags,
                                         @NonNull String publishAction) {
        Intent intent = new Intent(context, PublishIntentService.class);
        intent.putExtra(URL, url);
        intent.putExtra(BLOG_NAME, blogName);
        intent.putExtra(POST_TITLE, postTitle);
        intent.putExtra(POST_TAGS, postTags);
        intent.putExtra(ACTION, publishAction);

        context.startService(intent);
    }

    public static void startBirthdayListIntent(Context context,
                                               Calendar date) {
        Intent intent = new Intent(context, PublishIntentService.class);
        intent.putExtra(BIRTHDAY_DATE, date);
        intent.putExtra(ACTION, BIRTHDAY_LIST_BY_DATE_ACTION);

        context.startService(intent);
    }

    private void broadcastBirthdaysByDate(Intent intent) {
        Calendar birthday = (Calendar) intent.getSerializableExtra(BIRTHDAY_DATE);
        if (birthday == null) {
            birthday = Calendar.getInstance(Locale.US);
        }
        ArrayList<Pair<Birthday, TumblrPhotoPost>> list;
        try {
            list = BirthdayUtils.getPhotoPosts(getApplicationContext(), birthday);
        } catch (Exception ex) {
            // we can't use Collections.emptyList() because java.util.List isn't Serializable
            list = new ArrayList<>();
        }
        Intent countIntent = new Intent(BIRTHDAY_INTENT);
        countIntent.putExtra(RESULT_LIST1, list);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(countIntent);
    }
}
