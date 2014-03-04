package com.ternaryop.photoshelf.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;

import com.ternaryop.photoshelf.R;
import com.ternaryop.tumblr.Tumblr;

/**
 * Created by dave on 01/03/14.
 */
public class PublishIntentService extends IntentService {
    private static final String NOTIFICATION_TAG = "com.ternaryop.photoshelf.publish.error";
    private static final int NOTIFICATION_ID = 1;

    public static final String URL_OR_FILE = "urlOrFile";
    public static final String BLOG_NAME = "blogName";
    public static final String POST_TITLE = "postTitle";
    public static final String POST_TAGS = "postTags";
    public static final String PUBLISH_ACTION = "action";
    public static final String PUBLISH_ACTION_DRAFT = "draft";
    public static final String PUBLISH_ACTION_PUBLISH = "publish";

    public PublishIntentService() {
        super("publishIntent");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Object url = intent.getSerializableExtra(URL_OR_FILE);
        String selectedBlogName = intent.getStringExtra(BLOG_NAME);
        String postTitle = intent.getStringExtra(POST_TITLE);
        String postTags = intent.getStringExtra(POST_TAGS);
        String action = intent.getStringExtra(PUBLISH_ACTION);

        try {
            if (PUBLISH_ACTION_DRAFT.equals(action)) {
                Tumblr.getSharedTumblr(getApplicationContext()).draftPhotoPost(selectedBlogName,
                        url, postTitle, postTags);
            } else if (PUBLISH_ACTION_PUBLISH.equals(action)) {
                Tumblr.getSharedTumblr(getApplicationContext()).publishPhotoPost(selectedBlogName,
                        url, postTitle, postTags);
            }
        } catch (Exception ex) {
            try {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "publish_errors.txt");
                String date = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
                FileOutputStream fos = new FileOutputStream(file, true);
                PrintStream ps = new PrintStream(fos);
                ps.println(date + " Error on url " + url);
                ps.println(date + " tags " + postTags);
                ex.printStackTrace(ps);
                ps.flush();
                ps.close();
                fos.close();
            } catch (Exception e) {
            }
            notifyError(intent);
        }
            
    }

    private void notifyError(Intent intent) {
        Object url = intent.getSerializableExtra(URL_OR_FILE).toString();
        String postTags = intent.getStringExtra(POST_TAGS);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this)
                .setTicker(getString(R.string.upload_error_ticker))
                .setSmallIcon(R.drawable.stat_notify_error);

        builder.setContentText(postTags);
        // remove notification when user clicks on it
        builder.setAutoCancel(true);

        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        // add url.hashCode() to ensure every notification is shown
        notificationManager.notify(NOTIFICATION_TAG, NOTIFICATION_ID + url.hashCode(), notification);

    }

    private static void startActionIntent(Context context,
                                            Object urlOrFile,
                                            String blogName,
                                            String postTitle,
                                            String postTags,
                                            String publishAction) {
        Intent intent = new Intent(context, PublishIntentService.class);
        intent.putExtra(URL_OR_FILE, (Serializable)urlOrFile);
        intent.putExtra(BLOG_NAME, blogName);
        intent.putExtra(POST_TITLE, postTitle);
        intent.putExtra(POST_TAGS, postTags);
        intent.putExtra(PUBLISH_ACTION, publishAction);

        context.startService(intent);
    }

    public static void startSaveAsDraftIntent(Context context,
                                              Object urlOrFile,
                                              String blogName,
                                              String postTitle,
                                              String postTags) {
        startActionIntent(context, urlOrFile, blogName, postTitle, postTags, PUBLISH_ACTION_DRAFT);
    }

    public static void startPublishIntent(Context context,
                                              Object urlOrFile,
                                              String blogName,
                                              String postTitle,
                                              String postTags) {
        startActionIntent(context, urlOrFile, blogName, postTitle, postTags, PUBLISH_ACTION_PUBLISH);
    }
}
