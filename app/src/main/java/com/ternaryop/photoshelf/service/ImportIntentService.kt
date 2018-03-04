package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_FILE_PATH
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.photoshelf.util.notification.NOTIFICATION_ID_IMPORT_BIRTHDAY
import com.ternaryop.photoshelf.util.notification.NOTIFICATION_ID_IMPORT_POSTS
import com.ternaryop.photoshelf.util.notification.NotificationUtil
import com.ternaryop.photoshelf.util.notification.ProgressNotification

/**
 * An [IntentService] subclass for handling import/export actions
 */
class ImportIntentService : IntentService("ImportIntentService") {
    private lateinit var notificationUtil: NotificationUtil
    private lateinit var importer: Importer

    override fun onCreate() {
        super.onCreate()
        notificationUtil = NotificationUtil(this)
        importer = Importer(this, DropboxManager.getInstance(this))
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            when {
                ACTION_EXPORT_POSTS_CSV == action ->
                    handleExportPostsToCSV(intent.getStringExtra(EXTRA_FILE_PATH))
                ACTION_EXPORT_BIRTHDAYS_CSV == action ->
                    handleExportBirthdaysToCSV(intent.getStringExtra(EXTRA_FILE_PATH))
                ACTION_EXPORT_MISSING_BIRTHDAYS_CSV == action ->
                    handleExportMissingBirthdaysToCSV(intent.getStringExtra(EXTRA_FILE_PATH),
                        intent.getStringExtra(EXTRA_BLOG_NAME))
                ACTION_IMPORT_BIRTHDAYS_FROM_WEB == action ->
                    handleImportBirthdaysFromWeb(intent.getStringExtra(EXTRA_BLOG_NAME))
                ACTION_IMPORT_BIRTHDAYS_FROM_CSV == action ->
                    handleImportBirthdaysFromCSV(intent.getStringExtra(EXTRA_FILE_PATH))
                ACTION_IMPORT_POSTS_FROM_CSV == action ->
                    handleImportPostsFromCSV(intent.getStringExtra(EXTRA_FILE_PATH))
            }
        }
    }

    private fun handleExportPostsToCSV(exportPath: String) {
        try {
            val count = importer.exportPostsToCSV(exportPath)
            val content = resources.getQuantityString(R.plurals.exported_items, count, count)
            notificationUtil.notifyExport(content,
                "", "Post Export", R.drawable.stat_notify_import_export)
        } catch (e: Exception) {
            notificationUtil.notifyError(e, "Export", null)
        }
    }

    private fun handleExportBirthdaysToCSV(exportPath: String) {
        try {
            val count = importer.exportBirthdaysToCSV(exportPath)
            val content = resources.getQuantityString(R.plurals.exported_items, count, count)
            notificationUtil.notifyExport(content,
                "", "Birthdays Export", R.drawable.stat_notify_import_export)
        } catch (e: Exception) {
            notificationUtil.notifyError(e, "Export", null)
        }
    }

    private fun handleExportMissingBirthdaysToCSV(exportPath: String, blogName: String) {
        try {
            val count = importer.exportMissingBirthdaysToCSV(exportPath, blogName)
            val content = resources.getQuantityString(R.plurals.exported_items, count, count)
            notificationUtil.notifyExport(content,
                "", "Missing Birthdays Export", R.drawable.stat_notify_import_export)
        } catch (e: Exception) {
            notificationUtil.notifyError(e, "Export", null)
        }
    }

    private fun handleImportBirthdaysFromWeb(blogName: String) {
        val progressNotification = ProgressNotification(notificationUtil,
            R.string.import_missing_birthdays_from_web_title,
            NOTIFICATION_ID_IMPORT_BIRTHDAY)

        importer.importMissingBirthdaysFromWeb(blogName)
            .doOnNext { info ->
                progressNotification
                    .setProgress(info.max, info.progress, false)
                    .notify(info.items.size, R.plurals.item_found)
            }
            .takeLast(1)
            .subscribe({ info -> progressNotification.notifyFinish(info.items.size, R.plurals.imported_items) }
            ) { t -> notificationUtil.notifyError(t, "Import", null) }
    }

    private fun handleImportBirthdaysFromCSV(importPath: String) {
        try {
            importer.importBirthdays(importPath)
                .subscribe({ total ->
                    val content = resources.getQuantityString(R.plurals.imported_items, total!!, total)
                    notificationUtil.notifyExport(content,
                        "", "Birthdays Import", R.drawable.stat_notify_import_export)
                }
                ) { t -> notificationUtil.notifyError(t, "Import", null) }
        } catch (e: Exception) {
            notificationUtil.notifyError(e, "Import", null)
        }
    }

    private fun handleImportPostsFromCSV(importPath: String) {
        val progressNotification = ProgressNotification(
            notificationUtil,
            R.string.import_posts_from_csv_title,
            NOTIFICATION_ID_IMPORT_POSTS)

        try {
            importer.importPostsFromCSV(importPath)
                .doOnNext { progress ->
                    // update with less frequency otherwise randomly the notification inside
                    // subscribe() isn't respected (ie the progress remains visible)
                    if (progress!! % TWEAK_NOTIFICATION_UPDATE_FREQUENCY == 0) {
                        progressNotification
                            .setProgress(0, 0, true)
                            .notify(progress, R.plurals.item_found)
                    }
                }
                .takeLast(1)
                .subscribe({ progress -> progressNotification.notifyFinish(progress, R.plurals.imported_items) }
                ) { t -> notificationUtil.notifyError(t, "Import", null) }
        } catch (e: Exception) {
            notificationUtil.notifyError(e, "Import", null)
        }
    }

    companion object {
        private const val ACTION_EXPORT_POSTS_CSV = "exportPostsCSV"
        private const val ACTION_EXPORT_BIRTHDAYS_CSV = "exportBirthdaysCSV"
        private const val ACTION_EXPORT_MISSING_BIRTHDAYS_CSV = "exportMissingBirthdaysCSV"
        private const val ACTION_IMPORT_BIRTHDAYS_FROM_WEB = "importBirthdaysFromWeb"
        private const val ACTION_IMPORT_BIRTHDAYS_FROM_CSV = "importBirthdaysFromCSV"
        private const val ACTION_IMPORT_POSTS_FROM_CSV = "importPostsFromCSV"

        private const val TWEAK_NOTIFICATION_UPDATE_FREQUENCY = 1003

        fun startExportPostsCSV(context: Context, exportPath: String) {
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_EXPORT_POSTS_CSV
            intent.putExtra(EXTRA_FILE_PATH, exportPath)
            context.startService(intent)
        }

        fun startExportBirthdaysCSV(context: Context, exportPath: String) {
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_EXPORT_BIRTHDAYS_CSV
            intent.putExtra(EXTRA_FILE_PATH, exportPath)
            context.startService(intent)
        }

        fun startExportMissingBirthdaysCSV(context: Context, exportPath: String, blogName: String) {
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_EXPORT_MISSING_BIRTHDAYS_CSV
            intent.putExtra(EXTRA_FILE_PATH, exportPath)
            intent.putExtra(EXTRA_BLOG_NAME, blogName)
            context.startService(intent)
        }

        fun startImportBirthdaysFromWeb(context: Context, blogName: String) {
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_IMPORT_BIRTHDAYS_FROM_WEB
            intent.putExtra(EXTRA_BLOG_NAME, blogName)
            context.startService(intent)
        }

        fun startImportBirthdaysFromCSV(context: Context, importPath: String) {
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_IMPORT_BIRTHDAYS_FROM_CSV
            intent.putExtra(EXTRA_FILE_PATH, importPath)
            context.startService(intent)
        }

        fun startImportPostsFromCSV(context: Context, importPath: String) {
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_IMPORT_POSTS_FROM_CSV
            intent.putExtra(EXTRA_FILE_PATH, importPath)
            context.startService(intent)
        }
    }
}
