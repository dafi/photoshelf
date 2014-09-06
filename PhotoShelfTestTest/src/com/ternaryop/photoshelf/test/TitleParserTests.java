package com.ternaryop.photoshelf.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TitleParserTests {
    @Parameterized.Parameter(0)
    public String inputTitle;

    @Parameterized.Parameter(1)
    public String expectedTitle;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        BufferedReader inputReader = null;
        BufferedReader resultReader = null;
        ArrayList<Object[]> objects = new ArrayList<Object[]>();

        try {
            inputReader = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/dave/Dropbox/devel/android/photoShelf/tests/titleParser-test-input.txt"), "UTF-8"));
            resultReader = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/dave/Dropbox/devel/android/photoShelf/tests/titleParser-test-results.txt"), "UTF-8"));
            String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));

            String inputLine;
            String resultLine;
            while ((inputLine = inputReader.readLine()) != null && (resultLine = resultReader.readLine()) != null) {
                resultLine = resultLine.replace("%CURRENT_YEAR%", year);
                objects.add(new Object[] {inputLine, resultLine});
            }
        } finally {
            if (inputReader != null) try { inputReader.close(); } catch (IOException ignored) {}
            if (resultReader != null) try { resultReader.close(); } catch (IOException ignored) {}
        }
        return objects;
    }

    @Test
    public void testTitle() {
        TitleData titleData = TitleParser.instance().parseTitle(inputTitle);
        String formattedInput = titleData.format("<strong>", "</strong>", "<em>", "</em>");
        assertEquals(expectedTitle, formattedInput);
    }
}
