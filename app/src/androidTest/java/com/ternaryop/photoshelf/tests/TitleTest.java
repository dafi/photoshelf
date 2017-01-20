package com.ternaryop.photoshelf.tests;

import android.support.test.runner.AndroidJUnit4;

import com.ternaryop.photoshelf.parsers.AndroidTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;

/**
 * Created by dave on 21/09/14.
 * Ready to run test to used inside the emulator
 */
@RunWith(AndroidJUnit4.class)
public class TitleTest {
    @Test
    public void testTitle() {
        // fill the array with titles
        String arr[] = new String[] {
                ""
        };
        try {
            AndroidTitleParserConfig config = new AndroidTitleParserConfig(getTargetContext());
            StringBuilder sb = new StringBuilder();
            for (String i : arr) {
                TitleData titleData = TitleParser.instance(config).parseTitle(i, false);

                String formattedInput = titleData.format("<strong>", "</strong>", "<em>", "</em>");
                // i: input
                // s: simple without format
                // e: expected formatted
                sb.append("i:").append(i).append("\n")
                        .append("s:").append(titleData.format("", "", "", "")).append("\n")
                        .append("e:").append(formattedInput)
                        .append("\n\n");
                System.out.println("testTitle " + formattedInput);
            }
            String s = sb.toString();
            // setting the breakpoint to the println allows to copy to clipboard the whole string
            // this is necessary because the android log cuts the result
            System.out.println("whole string\n" + s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
