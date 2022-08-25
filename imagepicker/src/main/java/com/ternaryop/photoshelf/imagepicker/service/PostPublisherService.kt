package com.ternaryop.photoshelf.imagepicker.service

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ternaryop.photoshelf.imagepicker.R
import com.ternaryop.photoshelf.util.notification.notify
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.draftPhotoPost
import com.ternaryop.tumblr.publishPhotoPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.URI

class PostPublisherService(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val data = inputData.toPostPublisherData()
        try {
            data.newPublishClassInstance()?.also { onPublish(it, data) }
            publish(data)
        } catch (e: Exception) {
            with(applicationContext) {
                e.notify(
                    this,
                    data.postTags,
                    getString(R.string.upload_error_ticker),
                    data.url.hashCode()
                )
            }
        }
        return Result.success()
    }

    private fun publish(data: PostPublisherData) {
        try {
            if (data.action.isDraft) {
                TumblrManager.getInstance(applicationContext)
                    .draftPhotoPost(data.blogName, URI(data.url), data.postTitle, data.postTags)
            } else {
                TumblrManager.getInstance(applicationContext)
                    .publishPhotoPost(data.blogName, URI(data.url), data.postTitle, data.postTags)
            }
        } catch (e: Exception) {
            val retryIntent = RetryPublishNotificationBroadcastReceiver.createIntent(
                applicationContext,
                data
            )
            with(applicationContext) {
                e.notify(this, data.postTags, getString(R.string.retry), retryIntent, data.url.hashCode())
            }
        }
    }

    private fun onPublish(onPublishCallback: OnPublish, data: PostPublisherData) {
        val tagList = TumblrPost.tagsFromString(data.postTags)

        runBlocking(Dispatchers.IO) {
            try {
                onPublishCallback.publish(applicationContext, tagList)
            } catch (t: Throwable) {
                t.notify(applicationContext, "Publish", t.message)
            }
        }
    }

    companion object {
        fun startPublish(
            context: Context,
            data: PostPublisherData
        ) {
            val publishWorkRequest = OneTimeWorkRequestBuilder<PostPublisherService>()
                .setInputData(data.toWorkData())
                .build()
            WorkManager.getInstance(context).enqueue(publishWorkRequest)
        }
    }
}
