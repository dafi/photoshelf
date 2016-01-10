package com.ternaryop.photoshelf.tests;

import android.test.AndroidTestCase;

import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;

/**
 * Created by dave on 21/09/14.
 * Ready to run test to used inside the emulator
 */
public class TitleTest extends AndroidTestCase {
    public void testTitle() {
        // fill the array with titles
        String arr[] = new String[]{
                ""
        };
        try {
            AndroidTitleParserConfig config = new AndroidTitleParserConfig(getContext());
            for (String i : arr) {
                TitleData titleData = TitleParser.instance(config).parseTitle(i);

                String formattedInput = titleData.format("<strong>", "</strong>", "<em>", "</em>");
                System.out.println("testTitle " + formattedInput);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
