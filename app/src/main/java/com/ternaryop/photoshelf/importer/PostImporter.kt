package com.ternaryop.photoshelf.importer

import android.os.Environment
import android.support.annotation.PluralsRes
import android.widget.TextView
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.DbImport
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.db.Importer.Companion.fastPrintWriter
import com.ternaryop.photoshelf.db.PostTag
import com.ternaryop.photoshelf.db.PostTagDAO
import com.ternaryop.tumblr.TumblrPost
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.util.Locale

/**
 * Created by dave on 24/03/18.
 * Import and export tumblr posts
 */

private const val CSV_INDEX_ID = 0
private const val CSV_INDEX_BLOG_NAME = 1
private const val CSV_INDEX_TAG = 2
private const val CSV_INDEX_TIMESTAMP = 3
private const val CSV_INDEX_SHOW_ORDER = 4

private const val CSV_FILE_NAME = "tags.csv"

private val doNothingTransformer: ObservableTransformer<List<TumblrPost>, List<TumblrPost>>
    = ObservableTransformer { upstream -> upstream }

private val postTagCSVBuilder = object: CSVIterator.CSVBuilder<PostTag> {
    override fun parseCSVFields(fields: Array<String>): PostTag {
        return PostTag(
            fields[CSV_INDEX_ID].toLong(),
            fields[CSV_INDEX_BLOG_NAME],
            fields[CSV_INDEX_TAG],
            fields[CSV_INDEX_TIMESTAMP].toLong(),
            fields[CSV_INDEX_SHOW_ORDER].toInt())
    }
}

fun Importer.importPostsFromCSV(importPath: String): Observable<Int> {
    return DbImport(DBHelper.getInstance(context).bulkImportPostDAOWrapper)
        .importer(CSVIterator(importPath, postTagCSVBuilder), true)
}

fun Importer.exportPostsToCSV(exportPath: String): Int {
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
fun Importer.importFromTumblr(blogName: String,
    transformer: ObservableTransformer<List<TumblrPost>, List<TumblrPost>>?,
    textView: TextView?): Observable<List<TumblrPost>> {
    val post = DBHelper.getInstance(context).postTagDAO.findLastPublishedPost(blogName)

    textView?.text = context.getString(R.string.start_import_title)

    val postRetriever = PostRetriever(context)

    return postRetriever
        .readPhotoPosts(blogName, post?.publishTimestamp ?: 0)
        .compose<List<TumblrPost>>(transformer ?: doNothingTransformer)
        .doOnNext { updateText(textView, postRetriever.total, R.plurals.posts_read_count) }
        .observeOn(Schedulers.computation())
        .flatMap { postTags -> importToDB(postTags, textView) }
}

private fun Importer.importToDB(postTags: List<TumblrPost>, textView: TextView?): Observable<List<TumblrPost>> {
    return DbImport(DBHelper.getInstance(context).bulkImportPostDAOWrapper)
        .importer(PostTag.from(postTags).iterator(), false)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext { total -> updateText(textView, total!!, R.plurals.imported_items) }
        .takeLast(1)
        .flatMap { Observable.just(postTags) }
}

private fun Importer.updateText(textView: TextView?, total: Int, @PluralsRes id: Int) {
    textView?.text = context.resources.getQuantityString(
            id,
            total,
            total)
}

val Importer.Companion.postsPath: String
    get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .toString() + File.separator + CSV_FILE_NAME
