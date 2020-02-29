package com.ternaryop.photoshelf.birthday.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ternaryop.photoshelf.api.birthday.Birthday
import com.ternaryop.photoshelf.birthday.R
import com.ternaryop.photoshelf.birthday.util.createBirthdayImageFile
import com.ternaryop.photoshelf.birthday.util.createBirthdayPost
import com.ternaryop.photoshelf.util.notification.notify
import com.ternaryop.tumblr.android.TumblrManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.ArrayList

private const val PARAM_BIRTHDAY_FILE = "birthdayFile"
private const val PARAM_BLOG_NAME = "blogName"
private const val PARAM_PUBLISH_AS_DRAFT = "publishAsDraft"

/**
 * Created by dave on 01/03/14.
 * Intent used to add, retrieve or update birthdays
 * 02/25/20 - Converted to Worker to agree with new Android guidelines
 */
class BirthdayPublisherService(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    private val birthdayBitmap: Bitmap
        @Throws(IOException::class)
        get() = applicationContext.assets.open("cake.png").use { stream -> return BitmapFactory.decodeStream(stream) }

    override fun doWork(): Result {
        birthdaysPublish()
        return Result.success()
    }

    @Suppress("UNCHECKED_CAST")
    private fun birthdaysPublish() {
        val file = File(checkNotNull(inputData.getString(PARAM_BIRTHDAY_FILE)))
        val list: List<Birthday> = ObjectInputStream(FileInputStream(file)).use { it.readObject() } as List<Birthday>
        val blogName = inputData.getString(PARAM_BLOG_NAME) ?: return
        val publishAsDraft = inputData.getBoolean(PARAM_PUBLISH_AS_DRAFT, true)
        var name = ""

        try {
            val cakeImage = birthdayBitmap
            val tumblr = TumblrManager.getInstance(applicationContext)
            for (bday in list) {
                name = bday.name
                val imageFile = createBirthdayImageFile(cakeImage, bday, applicationContext.cacheDir)
                tumblr.createBirthdayPost(imageFile, bday, blogName, publishAsDraft)
            }
        } catch (e: Exception) {
            with(applicationContext) {
                e.notify(this, name, getString(R.string.birthday_publish_error_ticker, name, e.message))
            }
        } finally {
            file.delete()
        }
    }

    companion object {
        fun startPublish(
            context: Context,
            list: ArrayList<Birthday>,
            blogName: String,
            publishAsDraft: Boolean
        ) {
            if (list.isEmpty()) {
                return
            }
            // Worker data is persisted and doens't allow to pass serialized object
            // so we write birthdays into file
            val file = File(context.cacheDir, "bdays-${System.currentTimeMillis()}")
            ObjectOutputStream(FileOutputStream(file)).use { it.writeObject(list) }

            val data = workDataOf(
                PARAM_BIRTHDAY_FILE to file.absolutePath,
                PARAM_BLOG_NAME to blogName,
                PARAM_PUBLISH_AS_DRAFT to publishAsDraft
            )
            val publishWorkRequest = OneTimeWorkRequestBuilder<BirthdayPublisherService>()
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(publishWorkRequest)
        }
    }
}
