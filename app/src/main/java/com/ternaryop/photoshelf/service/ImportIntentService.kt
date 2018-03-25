package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.EXTRA_FILE_PATH
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.importer.notifyExportBirthdaysToCSV
import com.ternaryop.photoshelf.importer.notifyExportMissingBirthdaysToCSV
import com.ternaryop.photoshelf.importer.notifyExportPostsToCSV
import com.ternaryop.photoshelf.importer.notifyImportBirthdays
import com.ternaryop.photoshelf.importer.notifyImportBirthdaysFromWeb
import com.ternaryop.photoshelf.importer.notifyImportPostsFromCSV
import com.ternaryop.photoshelf.util.notification.NotificationUtil

/**
 * An [IntentService] subclass for handling import/export actions
 */
class ImportIntentService : IntentService("ImportIntentService") {
    override fun onHandleIntent(intent: Intent?) {
        val notificationUtil = NotificationUtil(this)
        when (intent?.action) {
            ACTION_IMPORT_POSTS_FROM_CSV ->
                Importer(this).notifyImportPostsFromCSV(intent.getStringExtra(EXTRA_FILE_PATH), notificationUtil)
            ACTION_EXPORT_POSTS_CSV ->
                Importer(this).notifyExportPostsToCSV(intent.getStringExtra(EXTRA_FILE_PATH), notificationUtil)
            ACTION_IMPORT_BIRTHDAYS_FROM_CSV ->
                Importer(this).notifyImportBirthdays(intent.getStringExtra(EXTRA_FILE_PATH), notificationUtil)
            ACTION_EXPORT_BIRTHDAYS_CSV ->
                Importer(this).notifyExportBirthdaysToCSV(intent.getStringExtra(EXTRA_FILE_PATH), notificationUtil)
            ACTION_EXPORT_MISSING_BIRTHDAYS_CSV ->
                Importer(this)
                    .notifyExportMissingBirthdaysToCSV(
                        intent.getStringExtra(EXTRA_FILE_PATH),
                        intent.getStringExtra(EXTRA_BLOG_NAME),
                        notificationUtil)
            ACTION_IMPORT_BIRTHDAYS_FROM_WEB ->
                Importer(this)
                    .notifyImportBirthdaysFromWeb(intent.getStringExtra(EXTRA_BLOG_NAME), notificationUtil)
        }
    }

    companion object {
        private const val ACTION_EXPORT_POSTS_CSV = "exportPostsCSV"
        private const val ACTION_EXPORT_BIRTHDAYS_CSV = "exportBirthdaysCSV"
        private const val ACTION_EXPORT_MISSING_BIRTHDAYS_CSV = "exportMissingBirthdaysCSV"
        private const val ACTION_IMPORT_BIRTHDAYS_FROM_WEB = "importBirthdaysFromWeb"
        private const val ACTION_IMPORT_BIRTHDAYS_FROM_CSV = "importBirthdaysFromCSV"
        private const val ACTION_IMPORT_POSTS_FROM_CSV = "importPostsFromCSV"

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
