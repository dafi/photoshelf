package com.ternaryop.photoshelf.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.db.BirthdayDAO;
import com.ternaryop.photoshelf.db.DBHelper;
import com.ternaryop.photoshelf.event.BirthdayEvent;
import com.ternaryop.photoshelf.util.NotificationUtil;
import com.ternaryop.photoshelf.util.log.Log;
import com.ternaryop.tumblr.Tumblr;
import com.ternaryop.tumblr.TumblrPhotoPost;
import org.greenrobot.eventbus.EventBus;

/**
 * Created by dave on 01/03/14.
 * Contains all methods used to publish posts
 */
public class PublishIntentService extends IntentService implements PhotoShelfIntentExtra {

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
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
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
            } else if (BIRTHDAY_PUBLISH_ACTION.equals(action)) {
                birthdaysPublish(intent);
            }
        } catch (Exception e) {
            logError(intent, e);
            notificationUtil.notifyError(e, postTags, getString(R.string.upload_error_ticker), url.hashCode());
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

    public static void startPublishBirthdayIntent(Context context,
                                                  @NonNull ArrayList<TumblrPhotoPost> list,
                                                  @NonNull String blogName,
                                                  boolean publishAsDraft) {
        if (list.isEmpty()) {
            return;
        }
        Intent intent = new Intent(context, PublishIntentService.class);
        intent.putExtra(LIST1, list);
        intent.putExtra(BLOG_NAME, blogName);
        intent.putExtra(BOOLEAN1, publishAsDraft);
        intent.putExtra(ACTION, BIRTHDAY_PUBLISH_ACTION);

        context.startService(intent);
    }

    private void broadcastBirthdaysByDate(Intent intent) {
        Calendar birthday = (Calendar) intent.getSerializableExtra(BIRTHDAY_DATE);
        if (birthday == null) {
            birthday = Calendar.getInstance(Locale.US);
        }
        List<Pair<Birthday, TumblrPhotoPost>> list;
        try {
            list = BirthdayUtils.getPhotoPosts(getApplicationContext(), birthday);
        } catch (Exception ex) {
            list = Collections.emptyList();
        }
        if (EventBus.getDefault().hasSubscriberForEvent(BirthdayEvent.class)) {
            EventBus.getDefault().post(new BirthdayEvent(list));
        }
    }

    private void birthdaysPublish(Intent intent) {
        @SuppressWarnings("unchecked") List<TumblrPhotoPost> posts = (List<TumblrPhotoPost>)intent.getSerializableExtra(LIST1);
        final String blogName = intent.getStringExtra(BLOG_NAME);
        final boolean publishAsDraft = intent.getBooleanExtra(BOOLEAN1, true);
        String name = "";

        try {
            final Bitmap cakeImage = getBirthdayBitmap();
            for (TumblrPhotoPost post : posts) {
                name = post.getTags().get(0);
                BirthdayUtils.createBirthdayPost(getApplicationContext(), cakeImage, post, blogName, publishAsDraft);
            }
        } catch (Exception e) {
            logError(intent, e);
            notificationUtil.notifyError(e, name, getString(R.string.birthday_publish_error_ticker, name, e.getMessage()));
        }
    }

    private Bitmap getBirthdayBitmap() throws IOException {
        try (InputStream is = getAssets().open("cake.png")) {
            return BitmapFactory.decodeStream(is);
        }
    }
}
