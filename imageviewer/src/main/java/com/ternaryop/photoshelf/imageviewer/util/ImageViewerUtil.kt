package com.ternaryop.photoshelf.imageviewer.util

import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.StringRes
import com.ternaryop.utils.dialog.showErrorDialog
import com.ternaryop.utils.intent.ShareChooserParams
import com.ternaryop.utils.intent.ShareUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    /**
     * Download [url] and save the content into the Pictures directory inside the [relativePath].
     * [relativePath] can contain subdirectories and must contain the file name
     */
    suspend fun download(context: Context, url: String, relativePath: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadQ(context, url, relativePath)
        } else {
            downloadLegacy(context, url, relativePath)
        }
    }

    private fun downloadLegacy(context: Context, url: String, relativePath: File) {
        val fileUri = Uri.fromFile(File(getPicturesDirectory(checkNotNull(relativePath.parentFile)), relativePath.name))
        val request = DownloadManager.Request(Uri.parse(url))
            .setDestinationUri(fileUri)
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private suspend fun downloadQ(context: Context, url: String, relativePath: File) = withContext(Dispatchers.IO) {
        val pictureRelativePath = File(Environment.DIRECTORY_PICTURES, relativePath.path)
        val values = ContentValues().apply {
            put(MediaStore.Images.ImageColumns.DISPLAY_NAME, pictureRelativePath.name)
            put(MediaStore.Images.ImageColumns.IS_PENDING, 1)
            put(MediaStore.Images.ImageColumns.RELATIVE_PATH, pictureRelativePath.parent)
        }
        val resolver = context.contentResolver
        val destUri = checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        downloadImageUrl(context.contentResolver, URL(url), destUri)
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(destUri, values, null, null)
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

    private suspend fun downloadImageUrl(
        contentResolver: ContentResolver,
        imageUrl: URL,
        uri: Uri
    ) = withContext(Dispatchers.IO) {
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

    private fun getPicturesDirectory(relativePath: File): File {
        val fullDirPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            relativePath.path
        )
        if (!fullDirPath.exists()) {
            fullDirPath.mkdirs()
        }
        return fullDirPath
    }
}
