package com.ternaryop.photoshelf.db

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.dropbox.core.v2.files.WriteMode
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.tumblr.android.TumblrManager
import com.ternaryop.tumblr.getFollowers
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
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

    fun copyFileToDropbox(exportPath: String) {
        val dropboxManager = DropboxManager.getInstance(context)

        if (dropboxManager.isLinked) {
            val exportFile = File(exportPath)
            FileInputStream(exportFile).use { stream ->
                // Autorename = true and Mode = OVERWRITE allow to overwrite
                // the file if it exists or create it if doesn't
                dropboxManager.client!!
                        .files()
                        .uploadBuilder(dropboxPath(exportFile))
                        .withAutorename(true)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(stream)
            }
        }
    }

    private fun dropboxPath(exportFile: File): String = "/${exportFile.name}"

    fun syncExportTotalUsersToCSV(exportPath: String, blogName: String) {
        // do not overwrite the entire file but append to the existing one
        PrintWriter(BufferedWriter(FileWriter(exportPath, true))).use { pw ->
            val time = ISO_8601_DATE.format(Calendar.getInstance().timeInMillis)
            val totalUsers = TumblrManager.getInstance(context).getFollowers(blogName).totalUsers
            pw.println("$time;$blogName;$totalUsers")
            pw.flush()
            copyFileToDropbox(exportPath)
        }
    }

    interface ImportProgressInfo<T> {
        val progress: Int
        val max: Int
        val items: MutableList<T>
    }

    class SimpleImportProgressInfo<T>(override val max: Int, val list: List<T> = emptyList()) : ImportProgressInfo<T> {
        override var progress: Int = 0
            internal set
        override val items: MutableList<T> = mutableListOf()

        override fun toString(): String = "progress $progress max $max items $items"
    }

    companion object {
        const val TITLE_PARSER_FILE_NAME = "titleParser.json"
        private const val TOTAL_USERS_FILE_NAME = "totalUsers.csv"

        private val ISO_8601_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun schedulers(): ObservableTransformer<List<TumblrPost>, List<TumblrPost>> {
            return ObservableTransformer { upstream ->
                upstream
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
            }
        }

        val titleParserPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + File.separator + TITLE_PARSER_FILE_NAME

        val totalUsersPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString() + File.separator + TOTAL_USERS_FILE_NAME
    }
}
