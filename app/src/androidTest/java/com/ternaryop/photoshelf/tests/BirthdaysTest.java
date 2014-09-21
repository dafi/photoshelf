package com.ternaryop.photoshelf.tests;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import android.os.Environment;
import android.util.Log;

import com.ternaryop.photoshelf.birthday.BirthdayUtils;
import com.ternaryop.photoshelf.db.Birthday;
import com.ternaryop.photoshelf.importer.CSVIterator;
import junit.framework.TestCase;

/**
 * Created by dave on 21/09/14.
 * Code to quickly run birthdays code, some methods aren't really test and never fail
 */
public class BirthdaysTest extends TestCase {
    public void testFindMissing() {
        String importPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "missingBirthdays.csv";

        ///////////////////////////////////////////////////////////////
        //////
        ////// To see log output you need to run the test in debug mode
        //////
        ///////////////////////////////////////////////////////////////
        try {
            CSVIterator<String> iterator = new CSVIterator<String>(importPath, new CSVIterator.CSVBuilder<String>() {

                @Override
                public String parseCSVFields(String[] fields) throws ParseException {
                    return fields[0];
                }
            });

            while (iterator.hasNext()) {
                String name = iterator.next();
                Birthday birthday = BirthdayUtils.searchBirthday(name, "");
                if (birthday != null) {
                    Log.d("testFindMissing", "MissingBirthdaysTest.findMissing " + birthday);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
