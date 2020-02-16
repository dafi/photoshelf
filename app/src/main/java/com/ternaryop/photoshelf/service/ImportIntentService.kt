package com.ternaryop.photoshelf.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.ternaryop.photoshelf.EXTRA_BLOG_NAME
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.importer.notifyImportBirthdaysFromWeb
import kotlinx.coroutines.runBlocking

/**
 * An [IntentService] subclass for handling import/export actions
 */
class ImportIntentService : AbsIntentService("ImportIntentService") {
    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_IMPORT_BIRTHDAYS_FROM_WEB -> runBlocking {
                Importer(applicationContext).notifyImportBirthdaysFromWeb()
            }
        }
    }

    companion object {
        private const val ACTION_IMPORT_BIRTHDAYS_FROM_WEB = "importBirthdaysFromWeb"

        fun startImportBirthdaysFromWeb(context: Context, blogName: String) {
            val intent = Intent(context, ImportIntentService::class.java)
            intent.action = ACTION_IMPORT_BIRTHDAYS_FROM_WEB
            intent.putExtra(EXTRA_BLOG_NAME, blogName)
            context.startService(intent)
        }
    }
}
