package com.ternaryop.photoshelf.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.importer.notifyImportBirthdaysFromWeb

private const val PARAM_ACTION = "action"
private const val PARAM_BLOG_NAME = "blogName"

/**
 * An [Worker] subclass for handling import/export actions
 */
class ImportService(context: Context, workerParameters: WorkerParameters) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        when (inputData.getString(PARAM_ACTION)) {
            ACTION_IMPORT_BIRTHDAYS_FROM_WEB -> Importer(applicationContext).notifyImportBirthdaysFromWeb()
        }
        return Result.success()
    }

    companion object {
        private const val ACTION_IMPORT_BIRTHDAYS_FROM_WEB = "importBirthdaysFromWeb"

        fun startImportBirthdaysFromWeb(context: Context, blogName: String) {
            val data = workDataOf(
                PARAM_ACTION to ACTION_IMPORT_BIRTHDAYS_FROM_WEB,
                PARAM_BLOG_NAME to blogName
            )
            val importWorkRequest = OneTimeWorkRequestBuilder<ImportService>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(importWorkRequest)
        }
    }
}
