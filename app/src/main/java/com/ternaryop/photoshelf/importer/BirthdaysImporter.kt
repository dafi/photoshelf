package com.ternaryop.photoshelf.importer

import android.os.Environment
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.db.Birthday
import com.ternaryop.photoshelf.db.BirthdayDAO
import com.ternaryop.photoshelf.db.DBHelper
import com.ternaryop.photoshelf.db.DbImport
import com.ternaryop.photoshelf.db.Importer
import io.reactivex.Observable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun Importer.importMissingBirthdaysFromWeb(blogName: String): Observable<Importer.ImportProgressInfo<Birthday>> {
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

private fun Importer.saveBirthdaysToDatabase(birthdays: List<Birthday>) {
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

private fun saveBirthdaysToFile(birthdays: List<Birthday>) {
    val fileName = "birthdays-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".csv"
    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        .toString() + File.separator + fileName

    Importer.fastPrintWriter(path).use { pw ->
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
