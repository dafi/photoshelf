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
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.widget.Toast;

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
import com.ternaryop.utils.ImageUtils;
import org.greenrobot.eventbus.EventBus;

/**
 * Created by dave on 01/03/14.
 * Contains all methods used to publish posts
 */
public class PublishIntentService extends IntentService implements PhotoShelfIntentExtra {
    private final static String ACTION_PUBLISH_DRAFT = "draft";
    private final static String ACTION_PUBLISH_PUBLISH = "publish";
    private final static String ACTION_BIRTHDAY_LIST_BY_DATE = "birthdayListByDate";
    private final static String ACTION_BIRTHDAY_PUBLISH = "birthdayPublish";
    private final static String ACTION_CHANGE_WALLPAPER = "changeWallpaper";

    private NotificationUtil notificationUtil;
    private final Handler handler;

    public PublishIntentService() {
        super("publishIntent");
        handler = new Handler();
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
        Uri url = intent.getParcelableExtra(EXTRA_URI);
        String selectedBlogName = intent.getStringExtra(EXTRA_BLOG_NAME);
        String postTitle = intent.getStringExtra(EXTRA_POST_TITLE);
        String postTags = intent.getStringExtra(EXTRA_POST_TAGS);
        String action = intent.getStringExtra(EXTRA_ACTION);

        try {
            addBithdateFromTags(postTags, selectedBlogName);
            if (ACTION_PUBLISH_DRAFT.equals(action)) {
                Tumblr.getSharedTumblr(getApplicationContext()).draftPhotoPost(selectedBlogName,
                        url, postTitle, postTags);
            } else if (ACTION_PUBLISH_PUBLISH.equals(action)) {
                Tumblr.getSharedTumblr(getApplicationContext()).publishPhotoPost(selectedBlogName,
                        url, postTitle, postTags);
            } else if (ACTION_BIRTHDAY_LIST_BY_DATE.equals(action)) {
                broadcastBirthdaysByDate(intent);
            } else if (ACTION_BIRTHDAY_PUBLISH.equals(action)) {
                birthdaysPublish(intent);
            } else if (ACTION_CHANGE_WALLPAPER.equals(action)) {
                changeWallpaper(url);
            }
        } catch (Exception e) {
            logError(intent, e);
            notificationUtil.notifyError(e, postTags, getString(R.string.upload_error_ticker), url.hashCode());
        }
    }

    private void changeWallpaper(Uri imageUrl) {
        try {
            WallpaperManager wpm = WallpaperManager.getInstance(this);
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            Bitmap bitmap = ImageUtils.getScaledBitmap(ImageUtils.readImageFromUrl(imageUrl.toString()), metrics.widthPixels , metrics.heightPixels, true);
            wpm.setBitmap(bitmap);
            showToast(R.string.wallpaper_changed_title);
        } catch (Exception e) {
            notificationUtil.notifyError(e, null, null);
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
        Uri url = intent.getParcelableExtra(EXTRA_URI);
        String postTags = intent.getStringExtra(EXTRA_POST_TAGS);

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
                                         boolean publish) {
        Intent intent = new Intent(context, PublishIntentService.class);
        intent.putExtra(EXTRA_URI, url);
        intent.putExtra(EXTRA_BLOG_NAME, blogName);
        intent.putExtra(EXTRA_POST_TITLE, postTitle);
        intent.putExtra(EXTRA_POST_TAGS, postTags);
        intent.putExtra(EXTRA_ACTION, publish ? ACTION_PUBLISH_PUBLISH : ACTION_PUBLISH_DRAFT);

        context.startService(intent);
    }

    public static void startBirthdayListIntent(Context context,
                                               Calendar date) {
        Intent intent = new Intent(context, PublishIntentService.class);
        intent.putExtra(EXTRA_BIRTHDAY_DATE, date);
        intent.putExtra(EXTRA_ACTION, ACTION_BIRTHDAY_LIST_BY_DATE);

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
        intent.putExtra(EXTRA_LIST1, list);
        intent.putExtra(EXTRA_BLOG_NAME, blogName);
        intent.putExtra(EXTRA_BOOLEAN1, publishAsDraft);
        intent.putExtra(EXTRA_ACTION, ACTION_BIRTHDAY_PUBLISH);

        context.startService(intent);
    }

    public static void startChangeWallpaperIntent(Context context,
                                                  @NonNull Uri imageUri) {
        Intent intent = new Intent(context, PublishIntentService.class);
        intent.putExtra(EXTRA_URI, imageUri);
        intent.putExtra(EXTRA_ACTION, ACTION_CHANGE_WALLPAPER);

        context.startService(intent);
    }

    private void broadcastBirthdaysByDate(Intent intent) {
        Calendar birthday = (Calendar) intent.getSerializableExtra(EXTRA_BIRTHDAY_DATE);
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
        @SuppressWarnings("unchecked") List<TumblrPhotoPost> posts = (List<TumblrPhotoPost>)intent.getSerializableExtra(EXTRA_LIST1);
        final String blogName = intent.getStringExtra(EXTRA_BLOG_NAME);
        final boolean publishAsDraft = intent.getBooleanExtra(EXTRA_BOOLEAN1, true);
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

    private void showToast(final @StringRes int res) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), res, Toast.LENGTH_LONG).show();
            }
        });
    }
}
