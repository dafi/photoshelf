package com.ternaryop.photoshelf.util.viewer

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.intent.ShareChooserParams
import com.ternaryop.utils.intent.ShareUtils
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Created by dave on 20/03/18.
 * Functions used by the image viewer to download and share image urls
 * Errors and successes are shown using UI elements (eg. dialogs and toasts)
 */
object ImageViewerUtil {
    fun download(context: Context, url: String, fileUri: Uri) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setDestinationUri(fileUri)
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        } catch (e: Exception) {
            e.showErrorDialog(context)
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String, @StringRes resultMessage: Int) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, resultMessage, Toast.LENGTH_SHORT).show()
    }

    suspend fun shareImage(context: Context, imageUrl: URL, shareChooserParams: ShareChooserParams) {
        try {
            downloadImageUrl(context.contentResolver, imageUrl, shareChooserParams.destFileUri)
            ShareUtils.showShareChooser(context, shareChooserParams)
        } catch (e: Exception) {
            e.showErrorDialog(context)
        }
    }

    fun buildSharePath(context: Context, url: String, subDirectory: String): File {
        val cacheDir = File(context.cacheDir, subDirectory).apply { mkdirs() }
        return File(cacheDir, buildFileName(url))
    }

    private suspend fun downloadImageUrl(contentResolver: ContentResolver, imageUrl: URL, uri: Uri) = coroutineScope {
        val connection = imageUrl.openConnection() as HttpURLConnection
        connection.inputStream.use { stream -> contentResolver.openOutputStream(uri)?.use { os -> stream.copyTo(os) } }
    }

    fun buildFileName(imageUrl: String, fileName: String? = null): String {
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