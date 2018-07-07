package com.ternaryop.photoshelf.importer

import android.os.Environment
import com.ternaryop.photoshelf.api.birthday.BirthdayManager
import com.ternaryop.photoshelf.api.birthday.BirthdayManager.Companion.MAX_BIRTHDAY_COUNT
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.BirthdayDAO
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.DbImport
import com.ternaryop.photoshelf.db.Importer
import com.ternaryop.photoshelf.util.network.ApiManager
import io.reactivex.Emitter
import io.reactivex.Observable
import io.reactivex.functions.BiConsumer
import java.io.File
import java.util.Locale
import java.util.concurrent.Callable

/**
 * Created by dave on 24/03/18.
 * Import and export birthdays
 */
private const val CSV_INDEX_NAME = 1
private const val CSV_INDEX_DATE = 2
private const val CSV_INDEX_BLOG_NAME = 3

private const val BIRTHDAYS_FILE_NAME = "birthdays.csv"
private const val MISSING_BIRTHDAYS_FILE_NAME = "missingBirthdays.csv"

fun Importer.importBirthdays(importPath: String): Observable<Int> {
    return DbImport(DBHelper.getInstance(context).birthdayDAO)
        .importer(CSVIterator(importPath, object : CSVIterator.CSVBuilder<Birthday> {
            override fun parseCSVFields(fields: Array<String>): Birthday {
                return Birthday(fields[CSV_INDEX_NAME], fields[CSV_INDEX_DATE], fields[CSV_INDEX_BLOG_NAME])
            }
        }), true)
}

fun Importer.exportBirthdaysToCSV(exportPath: String): Int {
    val db = DBHelper.getInstance(context).readableDatabase
    db.query(BirthdayDAO.TABLE_NAME, null, null, null, null, null, BirthdayDAO.NAME).use { c ->
        val pw = Importer.fastPrintWriter(exportPath)
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

typealias StringProgressInfo = Importer.SimpleImportProgressInfo<String>

fun Importer.importMissingBirthdaysFromWeb(blogName: String): Observable<StringProgressInfo> {
    return Observable.generate<StringProgressInfo, StringProgressInfo>(Callable {
        val params = BirthdayManager.FindParams(offset = 0, limit = MAX_BIRTHDAY_COUNT)
        val names = ApiManager.birthdayManager(context).findMissingNames(params)
        Importer.SimpleImportProgressInfo(names.size, names)
        },
        BiConsumer { iterator: StringProgressInfo, emitter: Emitter<StringProgressInfo> ->
            if (iterator.progress < iterator.max) {
                val name = iterator.list[iterator.progress]

                BirthdayUtils.searchBirthday(context, name)?.also { iterator.items.add(name) }

                ++iterator.progress
                emitter.onNext(iterator)
            } else {
                emitter.onNext(iterator)
                emitter.onComplete()
            }
    })
}

fun Importer.exportMissingBirthdaysToCSV(exportPath: String, tumblrName: String): Int {
    Importer.fastPrintWriter(exportPath).use { pw ->
        val list = DBHelper.getInstance(context).birthdayDAO.getNameWithoutBirthDays(tumblrName)
        for (name in list) {
            pw.println(name)
        }
        pw.flush()

        copyFileToDropbox(exportPath)
        return list.size
    }
}

val Importer.Companion.missingBirthdaysPath: String
    get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .toString() + File.separator + MISSING_BIRTHDAYS_FILE_NAME

val Importer.Companion.birthdaysPath: String
    get() = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .toString() + File.separator + BIRTHDAYS_FILE_NAME
