package com.ternaryop.photoshelf.db

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.ternaryop.photoshelf.R
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getFollowers
import com.ternaryop.utils.dropbox.DropboxManager
import com.ternaryop.utils.dropbox.copyFile
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Importer constructor(val context: Context) {

    fun importFile(importPath: String, contextFileName: String) {
        try {
            copyFileToContext(importPath, contextFileName)
            Toast.makeText(context, context.getString(R.string.importSuccess), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun copyFileToContext(fullPath: String, contextFileName: String) {
        context.openFileOutput(contextFileName, 0).use { out ->
            File(fullPath).forEachBlock { buffer, bytesRead -> out.write(buffer, 0, bytesRead)}
        }
    }

    fun syncExportTotalUsersToCSV(exportPath: String, blogName: String) {
        // do not overwrite the entire file but append to the existing one
        PrintWriter(BufferedWriter(FileWriter(exportPath, true))).use { pw ->
            val time = ISO_8601_DATE.format(Calendar.getInstance().timeInMillis)
            val totalUsers = TumblrManager.getInstance(context).getFollowers(blogName).totalUsers
            pw.println("$time;$blogName;$totalUsers")
            pw.flush()
            DropboxManager.getInstance(context).copyFile(exportPath)
        }
    }

    interface ImportProgressInfo<T> {
        var progress: Int
        var max: Int
        var items: MutableList<T>
    }

    class SimpleImportProgressInfo<T>(override var max: Int = 0, val list: MutableList<T> = mutableListOf())
        : ImportProgressInfo<T> {
        override var progress: Int = 0
        override var items: MutableList<T> = mutableListOf()

        override fun toString(): String = "progress $progress max $max items $items"
    }

    companion object {
        private const val TOTAL_USERS_FILE_NAME = "totalUsers.csv"

        private val ISO_8601_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun schedulers(): ObservableTransformer<List<TumblrPost>, List<TumblrPost>> {
            return ObservableTransformer { upstream ->
                upstream
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
            }
        }

        val totalUsersPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + File.separator + TOTAL_USERS_FILE_NAME
    }
}
