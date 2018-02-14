package com.ternaryop.photoshelf.db

import android.content.Context
import android.os.Environment
import android.support.annotation.PluralsRes
import android.widget.TextView
import android.widget.Toast
import com.dropbox.core.v2.files.WriteMode
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.dropbox.DropboxManager
import com.ternaryop.photoshelf.importer.CSVIterator
import com.ternaryop.photoshelf.importer.CSVIterator.CSVBuilder
import com.ternaryop.photoshelf.importer.PostRetriever
import com.ternaryop.tumblr.Tumblr
import com.ternaryop.tumblr.TumblrPost
import com.ternaryop.utils.IOUtils
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Importer constructor(private val context: Context, private val dropboxManager: DropboxManager? = null) {

    @Throws(IOException::class)
    fun importPostsFromCSV(importPath: String): Observable<Int> {
        return DbImport(DBHelper.getInstance(context).bulkImportPostDAOWrapper)
                .importer(CSVIterator(importPath, PostTagCSVBuilder()), true)
    }

    @Throws(Exception::class)
    fun exportPostsToCSV(exportPath: String): Int {
        DBHelper.getInstance(context).postTagDAO.cursorExport().use { c ->
            val pw = fastPrintWriter(exportPath)
            var count = 0
            while (c.moveToNext()) {
                pw.println(String.format(Locale.US, "%1\$d;%2\$s;%3\$s;%4\$d;%5\$d",
                        c.getLong(c.getColumnIndex(PostTagDAO._ID)),
                        c.getString(c.getColumnIndex(PostTagDAO.TUMBLR_NAME)),
                        c.getString(c.getColumnIndex(PostTagDAO.TAG)),
                        c.getLong(c.getColumnIndex(PostTagDAO.PUBLISH_TIMESTAMP)),
                        c.getLong(c.getColumnIndex(PostTagDAO.SHOW_ORDER))
                ))
                ++count
            }
            pw.flush()
            pw.close()

            copyFileToDropbox(exportPath)
            return count
        }
    }

    /**
     * Create the Observable to import newer posts and inserted them into database
     * @param blogName the blog name
     * @param transformer used to set the schedulers to used
     * @param textView the textView used to show the progress, can be null
     * @return the Observable
     */
    fun importFromTumblr(blogName: String,
                         transformer: ObservableTransformer<List<TumblrPost>, List<TumblrPost>>?,
                         textView: TextView?): Observable<List<TumblrPost>> {
        val post = DBHelper.getInstance(context).postTagDAO.findLastPublishedPost(blogName)

        if (textView != null) {
            textView.text = context.getString(R.string.start_import_title)
        }

        val postRetriever = PostRetriever(context)

        return postRetriever
                .readPhotoPosts(blogName, post?.publishTimestamp ?: 0)
                .compose<List<TumblrPost>>(transformer ?: DO_NOTHING_TRANSFORMER)
                .doOnNext { updateText(textView, postRetriever.total, R.plurals.posts_read_count) }
                .observeOn(Schedulers.computation())
                .flatMap { postTags -> importToDB(postTags, textView) }
    }

    private fun importToDB(postTags: List<TumblrPost>, textView: TextView?): Observable<List<TumblrPost>> {
        return DbImport(DBHelper.getInstance(context).bulkImportPostDAOWrapper)
                .importer(PostTag.from(postTags).iterator(), false)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { total -> updateText(textView, total!!, R.plurals.imported_items) }
                .takeLast(1)
                .flatMap { Observable.just(postTags) }
    }

    private fun updateText(textView: TextView?, total: Int, @PluralsRes id: Int) {
        if (textView != null) {
            val message = context.resources.getQuantityString(
                    id,
                    total,
                    total)
            textView.text = message
        }
    }

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
            File(fullPath).forEachBlock({ buffer, bytesRead ->
                out.write(buffer, 0, bytesRead)
            })
        }
    }

    fun importBirthdays(importPath: String): Observable<Int> {
        return DbImport(DBHelper.getInstance(context).birthdayDAO)
                .importer(CSVIterator(importPath, object : CSVBuilder<Birthday> {
                    override fun parseCSVFields(fields: Array<String>): Birthday {
                        return Birthday(fields[1], fields[2], fields[3])
                    }
                }), true)
    }

    fun exportBirthdaysToCSV(exportPath: String): Int {
        val db = DBHelper.getInstance(context).readableDatabase
        db.query(BirthdayDAO.TABLE_NAME, null, null, null, null, null, BirthdayDAO.NAME).use { c ->
            val pw = fastPrintWriter(exportPath)
            var id: Long = 1
            var count = 0
            while (c.moveToNext()) {
                val birthdate = c.getString(c.getColumnIndex(BirthdayDAO.BIRTH_DATE))
                // ids are recomputed
                val csvLine = String.format(Locale.US,
                        "%1\$d;%2\$s;%3\$s;%4\$s",
                        id++,
                        c.getString(c.getColumnIndex(BirthdayDAO.NAME)),
                        birthdate ?: "",
                        c.getString(c.getColumnIndex(BirthdayDAO.TUMBLR_NAME))
                )
                pw.println(csvLine)
                ++count
            }
            pw.flush()
            pw.close()

            copyFileToDropbox(exportPath)
            return count
        }
    }

    fun importMissingBirthdaysFromWeb(blogName: String): Observable<ImportProgressInfo<Birthday>> {
        val birthdayDAO = DBHelper.getInstance(context).birthdayDAO
        val names = birthdayDAO.getNameWithoutBirthDays(blogName)
        val info = SimpleImportProgressInfo<Birthday>(names.size)
        val nameIterator = names.iterator()

        return Observable.generate { emitter ->
            if (nameIterator.hasNext()) {
                ++info.progress
                val name = nameIterator.next()
                try {
                    val birthday = BirthdayUtils.searchBirthday(context, name, blogName)
                    if (birthday != null) {
                        info.items.add(birthday)
                    }
                } catch (e: Exception) {
                    // simply skip
                }

                emitter.onNext(info)
            } else {
                saveBirthdaysToDatabase(info.items)
                saveBirthdaysToFile(info.items)
                emitter.onNext(info)
                emitter.onComplete()
            }
        }
    }

    private fun saveBirthdaysToDatabase(birthdays: List<Birthday>) {
        val birthdayDAO = DBHelper.getInstance(context).birthdayDAO
        val db = birthdayDAO.dbHelper.writableDatabase
        try {
            db.beginTransaction()
            for (birthday in birthdays) {
                birthdayDAO.insert(birthday)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    @Throws(IOException::class)
    private fun saveBirthdaysToFile(birthdays: List<Birthday>) {
        val fileName = "birthdays-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".csv"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName

        fastPrintWriter(path).use { pw ->
            for (birthday in birthdays) {
                pw.println(String.format(Locale.US, "%1\$d;%2\$s;%3\$s;%4\$s",
                        1L,
                        birthday.name,
                        Birthday.toIsoFormat(birthday.birthDate!!),
                        birthday.tumblrName))
            }
            pw.flush()
        }
    }

    @Throws(Exception::class)
    fun exportMissingBirthdaysToCSV(exportPath: String, tumblrName: String): Int {
        fastPrintWriter(exportPath).use { pw ->
            val list = DBHelper.getInstance(context).birthdayDAO.getNameWithoutBirthDays(tumblrName)
            for (name in list) {
                pw.println(name)
            }
            pw.flush()

            copyFileToDropbox(exportPath)
            return list.size
        }
    }

    internal class PostTagCSVBuilder : CSVBuilder<PostTag> {
        override fun parseCSVFields(fields: Array<String>): PostTag {
            return PostTag(
                    fields[0].toLong(),
                    fields[1],
                    fields[2],
                    fields[3].toLong(),
                    fields[4].toInt())
        }
    }

    @Throws(Exception::class)
    private fun copyFileToDropbox(exportPath: String) {
        if (dropboxManager!!.isLinked) {
            val exportFile = File(exportPath)
            FileInputStream(exportFile).use { stream ->
                // Autorename = true and Mode = OVERWRITE allow to overwrite the file if it exists or create it if doesn't
                dropboxManager.client!!
                        .files()
                        .uploadBuilder(dropboxPath(exportFile))
                        .withAutorename(true)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(stream)
            }
        }
    }

    private fun dropboxPath(exportFile: File): String {
        return "/" + exportFile.name
    }

    @Throws(Exception::class)
    fun syncExportTotalUsersToCSV(exportPath: String, blogName: String) {
        // do not overwrite the entire file but append to the existing one
        PrintWriter(BufferedWriter(FileWriter(exportPath, true))).use { pw ->
            val time = ISO_8601_DATE.format(Calendar.getInstance().timeInMillis)
            val totalUsers = Tumblr.getSharedTumblr(context)
                    .getFollowers(blogName, null, null)
                    .totalUsers
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

    inner class SimpleImportProgressInfo<T>(override val max: Int) : ImportProgressInfo<T> {
        override var progress: Int = 0
            internal set
        override val items: MutableList<T> = mutableListOf()

        override fun toString(): String = "progress $progress max $max items $items"
    }

    companion object {
        private const val CSV_FILE_NAME = "tags.csv"
        const val TITLE_PARSER_FILE_NAME = "titleParser.json"
        private const val BIRTHDAYS_FILE_NAME = "birthdays.csv"
        private const val MISSING_BIRTHDAYS_FILE_NAME = "missingBirthdays.csv"
        private const val TOTAL_USERS_FILE_NAME = "totalUsers.csv"

        private val ISO_8601_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val DO_NOTHING_TRANSFORMER: ObservableTransformer<List<TumblrPost>, List<TumblrPost>> = ObservableTransformer { upstream -> upstream }

        fun schedulers(): ObservableTransformer<List<TumblrPost>, List<TumblrPost>> {
            return ObservableTransformer { upstream ->
                upstream
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
            }
        }

        /**
         * If necessary rename exportPath
         * @param exportPath the export path to use as prefix
         * @return the original passed parameter if exportPath doesn't exist or a new unique path
         */
        fun getExportPath(exportPath: String): String {
            val newPath = IOUtils.generateUniqueFileName(exportPath)
            if (newPath != exportPath) {
                File(exportPath).renameTo(File(newPath))
            }
            return exportPath
        }

        val missingBirthdaysPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + MISSING_BIRTHDAYS_FILE_NAME

        val postsPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + CSV_FILE_NAME

        val titleParserPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + TITLE_PARSER_FILE_NAME

        val birthdaysPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + BIRTHDAYS_FILE_NAME

        val totalUsersPath: String
            get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + TOTAL_USERS_FILE_NAME

        /**
         * Create a PrintWriter disabling the flush to speedup writing
         * @param path the destination path
         * @return the created PrintWriter
         * @throws IOException the thrown exception
         */
        @Throws(IOException::class)
        fun fastPrintWriter(path: String): PrintWriter {
            return PrintWriter(BufferedWriter(FileWriter(path)), false)
        }
    }
}
