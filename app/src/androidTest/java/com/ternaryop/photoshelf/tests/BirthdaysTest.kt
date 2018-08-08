package com.ternaryop.photoshelf.tests

import android.app.Activity
import android.os.Environment
import android.util.Log
import com.ternaryop.photoshelf.importer.CSVIterator
import com.ternaryop.photoshelf.util.network.ApiManager
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
                ApiManager.birthdayService(Activity()).getByName(name, true)
                    .subscribe { birthday ->
                        Log.d("testFindMissing", "MissingBirthdaysTest.findMissing $birthday")
                    }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
