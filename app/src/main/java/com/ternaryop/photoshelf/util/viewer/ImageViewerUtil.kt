package com.ternaryop.photoshelf.util.viewer

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.support.annotation.StringRes
import android.widget.Toast
import com.ternaryop.photoshelf.AppSupport
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.util.text.fromHtml
import com.ternaryop.utils.DialogUtils
import com.ternaryop.utils.IOUtils
import com.ternaryop.utils.ShareUtils
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Created by dave on 20/03/18.
 * Functions used by the image viewer to download and share image urls
 * Errors and successes are shown using UI elements (eg. dialogs and toasts)
 */
object ImageViewerUtil {
    fun download(context: Context, url: String, suggestedFileName: String? = null) {
        try {
            val fileName = buildFileName(url, suggestedFileName)
            val request = DownloadManager.Request(Uri.parse(url))
                .setDestinationUri(Uri.fromFile(File(AppSupport.picturesDirectory, fileName)))
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        } catch (e: Exception) {
            DialogUtils.showErrorDialog(context, e)
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String, @StringRes resultMessage: Int) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.primaryClip = ClipData.newPlainText(label, text)
        Toast.makeText(context, resultMessage, Toast.LENGTH_SHORT).show()
    }

    fun shareImage(context: Context, url: String, suggestedTitle: String? = null) {
        try {
            val fileName = buildFileName(url)
            // write to a public location otherwise the called app can't access to file
            val destFile = File(AppSupport.picturesDirectory, fileName)
            downloadImageUrl(URL(url), destFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    ShareUtils.shareImage(context,
                        destFile.absolutePath,
                        "image/jpeg",
                        suggestedTitle?.fromHtml()?.toString() ?: "",
                        context.getString(R.string.share_image_title))
                }
                ) { throwable -> DialogUtils.showErrorDialog(context, throwable) }
        } catch (e: Exception) {
            DialogUtils.showErrorDialog(context, e)
        }
    }

    private fun downloadImageUrl(imageUrl: URL, destFile: File): Completable {
        return Completable.fromCallable {
            val connection = imageUrl.openConnection() as HttpURLConnection
            connection.inputStream.use { stream -> FileOutputStream(destFile).use { os -> IOUtils.copy(stream, os) } }
            null
        }
    }

    private fun buildFileName(imageUrl: String, fileName: String? = null): String {
        if (fileName == null) {
            var nameFromUrl = URI(imageUrl).path
            val index = nameFromUrl.lastIndexOf('/')
            if (index != -1) {
                nameFromUrl = nameFromUrl.substring(index + 1)
            }
            return nameFromUrl
        }
        val index = imageUrl.lastIndexOf(".")
        // append extension with "."
        return if (index != -1) {
            fileName + imageUrl.substring(index)
        } else fileName
    }
}