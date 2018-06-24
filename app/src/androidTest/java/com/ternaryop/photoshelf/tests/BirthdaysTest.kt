package com.ternaryop.photoshelf.tests

import android.app.Activity
import android.os.Environment
import android.util.Log
import com.ternaryop.photoshelf.birthday.BirthdayUtils
import com.ternaryop.photoshelf.importer.CSVIterator
import junit.framework.TestCase
import java.io.File
import java.io.IOException
import java.text.ParseException

/**
 * Created by dave on 21/09/14.
 * Code to quickly run birthdays code, some methods aren't really test and never fail
 */
class BirthdaysTest : TestCase() {
    fun testFindMissing() {
        val importPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "missingBirthdays.csv"

        ///////////////////////////////////////////////////////////////
        //////
        ////// To see log output you need to run the test in debug mode
        //////
        ///////////////////////////////////////////////////////////////
        try {
            val iterator = CSVIterator(importPath, object : CSVIterator.CSVBuilder<String> {

                @Throws(ParseException::class)
                override fun parseCSVFields(fields: Array<String>): String {
                    return fields[0]
                }
            })

            while (iterator.hasNext()) {
                val name = iterator.next()
                val birthday = BirthdayUtils.searchBirthday(Activity(), name)
                if (birthday != null) {
                    Log.d("testFindMissing", "MissingBirthdaysTest.findMissing $birthday")
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
