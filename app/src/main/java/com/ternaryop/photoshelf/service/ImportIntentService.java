package com.ternaryop.photoshelf.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.PluralsRes;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat;

import com.ternaryop.photoshelf.R;
import com.ternaryop.photoshelf.db.Importer;
import com.ternaryop.photoshelf.dropbox.DropboxManager;
import com.ternaryop.photoshelf.util.NotificationUtil;

import static com.ternaryop.photoshelf.util.NotificationUtil.NOTIFICATION_ID_IMPORT_BIRTHDAY;
import static com.ternaryop.photoshelf.util.NotificationUtil.NOTIFICATION_ID_IMPORT_POSTS;

/**
 * An {@link IntentService} subclass for handling import/export actions
 */
public class ImportIntentService extends IntentService implements PhotoShelfIntentExtra {

    private static final String ACTION_EXPORT_POSTS_CSV = "exportPostsCSV";
    private static final String ACTION_EXPORT_BIRTHDAYS_CSV = "exportBirthdaysCSV";
    private static final String ACTION_EXPORT_MISSING_BIRTHDAYS_CSV = "exportMissingBirthdaysCSV";
    private static final String ACTION_IMPORT_BIRTHDAYS_FROM_WEB = "importBirthdaysFromWeb";
    private static final String ACTION_IMPORT_BIRTHDAYS_FROM_CSV = "importBirthdaysFromCSV";
    private static final String ACTION_IMPORT_POSTS_FROM_CSV = "importPostsFromCSV";

    private NotificationUtil notificationUtil;
    private Importer importer;

    public ImportIntentService() {
        super("ImportIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationUtil = new NotificationUtil(this);
        importer = new Importer(this, DropboxManager.getInstance(this));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_EXPORT_POSTS_CSV.equals(action)) {
                handleExportPostsToCSV(intent.getStringExtra(EXTRA_FILE_PATH));
            } else if (ACTION_EXPORT_BIRTHDAYS_CSV.equals(action)) {
                handleExportBirthdaysToCSV(intent.getStringExtra(EXTRA_FILE_PATH));
            } else if (ACTION_EXPORT_MISSING_BIRTHDAYS_CSV.equals(action)) {
                handleExportMissingBirthdaysToCSV(intent.getStringExtra(EXTRA_FILE_PATH), intent.getStringExtra(EXTRA_BLOG_NAME));
            } else if (ACTION_IMPORT_BIRTHDAYS_FROM_WEB.equals(action)) {
                handleImportBirthdaysFromWeb(intent.getStringExtra(EXTRA_BLOG_NAME));
            } else if (ACTION_IMPORT_BIRTHDAYS_FROM_CSV.equals(action)) {
                handleImportBirthdaysFromCSV(intent.getStringExtra(EXTRA_FILE_PATH));
            } else if (ACTION_IMPORT_POSTS_FROM_CSV.equals(action)) {
                handleImportPostsFromCSV(intent.getStringExtra(EXTRA_FILE_PATH));
            }
        }
    }

    public static void startExportPostsCSV(Context context, final String exportPath) {
        Intent intent = new Intent(context, ImportIntentService.class);
        intent.setAction(ACTION_EXPORT_POSTS_CSV);
        intent.putExtra(EXTRA_FILE_PATH, exportPath);
        context.startService(intent);
    }

    public static void startExportBirthdaysCSV(Context context, final String exportPath) {
        Intent intent = new Intent(context, ImportIntentService.class);
        intent.setAction(ACTION_EXPORT_BIRTHDAYS_CSV);
        intent.putExtra(EXTRA_FILE_PATH, exportPath);
        context.startService(intent);
    }

    public static void startExportMissingBirthdaysCSV(Context context, final String exportPath, final String blogName) {
        Intent intent = new Intent(context, ImportIntentService.class);
        intent.setAction(ACTION_EXPORT_MISSING_BIRTHDAYS_CSV);
        intent.putExtra(EXTRA_FILE_PATH, exportPath);
        intent.putExtra(EXTRA_BLOG_NAME, blogName);
        context.startService(intent);
    }

    public static void startImportBirthdaysFromWeb(Context context, final String blogName) {
        Intent intent = new Intent(context, ImportIntentService.class);
        intent.setAction(ACTION_IMPORT_BIRTHDAYS_FROM_WEB);
        intent.putExtra(EXTRA_BLOG_NAME, blogName);
        context.startService(intent);
    }

    public static void startImportBirthdaysFromCSV(Context context, final String importPath) {
        Intent intent = new Intent(context, ImportIntentService.class);
        intent.setAction(ACTION_IMPORT_BIRTHDAYS_FROM_CSV);
        intent.putExtra(EXTRA_FILE_PATH, importPath);
        context.startService(intent);
    }

    public static void startImportPostsFromCSV(Context context, final String importPath) {
        Intent intent = new Intent(context, ImportIntentService.class);
        intent.setAction(ACTION_IMPORT_POSTS_FROM_CSV);
        intent.putExtra(EXTRA_FILE_PATH, importPath);
        context.startService(intent);
    }

    private void handleExportPostsToCSV(final String exportPath) {
        try {
            int count = importer.exportPostsToCSV(exportPath);
            String content = getResources().getQuantityString(R.plurals.exported_items, count, count);
            notificationUtil.notifyExport(content, "Postsssss", "Post Export", R.drawable.stat_notify_import_export);
        } catch (Exception e) {
            notificationUtil.notifyError(e, "Export", null);
        }
    }

    private void handleExportBirthdaysToCSV(final String exportPath) {
        try {
            int count = importer.exportBirthdaysToCSV(exportPath);
            String content = getResources().getQuantityString(R.plurals.exported_items, count, count);
            notificationUtil.notifyExport(content, "", "Birthdays Export", R.drawable.stat_notify_import_export);
        } catch (Exception e) {
            notificationUtil.notifyError(e, "Export", null);
        }
    }

    private void handleExportMissingBirthdaysToCSV(final String exportPath, final String blogName) {
        try {
            int count = importer.exportMissingBirthdaysToCSV(exportPath, blogName);
            String content = getResources().getQuantityString(R.plurals.exported_items, count, count);
            notificationUtil.notifyExport(content, "", "Missing Birthdays Export", R.drawable.stat_notify_import_export);
        } catch (Exception e) {
            notificationUtil.notifyError(e, "Export", null);
        }
    }

    private void handleImportBirthdaysFromWeb(String blogName) {
        final NotificationCompat.Builder builder = createNotificationBuilder(R.string.import_missing_birthdays_from_web_title);

        importer.importMissingBirthdaysFromWeb(blogName)
                .doOnNext(info -> updateNotification(builder, info.getMax(), info.getProgress(), false, info.getItems().size(), R.plurals.item_found, NOTIFICATION_ID_IMPORT_BIRTHDAY))
                .takeLast(1)
                .subscribe(info -> updateNotification(builder, 0, 0, false, info.getItems().size(), R.plurals.imported_items, NOTIFICATION_ID_IMPORT_BIRTHDAY),
                        t -> notificationUtil.notifyError(t, "Import", null));
    }

    private void handleImportBirthdaysFromCSV(String importPath) {
        try {
            importer.importBirthdays(importPath)
                    .subscribe(total -> {
                                String content = getResources().getQuantityString(R.plurals.imported_items, total, total);
                                notificationUtil.notifyExport(content, "", "Birthdays Import", R.drawable.stat_notify_import_export);
                            },
                            t -> notificationUtil.notifyError(t, "Import", null));
        } catch (Exception e) {
            notificationUtil.notifyError(e, "Import", null);
        }
    }

    private void handleImportPostsFromCSV(String importPath) {
        final NotificationCompat.Builder builder = createNotificationBuilder(R.string.import_posts_from_csv_title);
        try {
            importer.importPostsFromCSV(importPath)
                    .doOnNext(progress -> {
                        // update with less frequency otherwise randomnly the notification inside subscribe() isn't respected (ie the progress remains visible)
                        if ((progress % 1003) == 0) {
                            updateNotification(builder, 0, 0, true, progress, R.plurals.item_found, NOTIFICATION_ID_IMPORT_POSTS);
                        }
                    })
                    .takeLast(1)
                    .subscribe(progress -> updateNotification(builder, 0, 0, false, progress, R.plurals.imported_items, NOTIFICATION_ID_IMPORT_POSTS),
                            t -> notificationUtil.notifyError(t, "Import", null));
        } catch (Exception e) {
            notificationUtil.notifyError(e, "Import", null);
        }
    }

    private NotificationCompat.Builder createNotificationBuilder(@StringRes int titleId) {
        return notificationUtil.createNotification(
                "",
                "",
                null,
                R.drawable.stat_notify_import_export)
                .setContentTitle(getString(titleId));
    }

    private void updateNotification(NotificationCompat.Builder builder, int max, int progress, boolean indeterminate, int foundItems, @PluralsRes int pluralId, int notificationId) {
        builder
                .setProgress(max, progress, indeterminate)
                .setContentText(getResources().getQuantityString(pluralId, foundItems, foundItems));
        notificationUtil.getNotificationManager().notify(notificationId, builder.build());
    }
}