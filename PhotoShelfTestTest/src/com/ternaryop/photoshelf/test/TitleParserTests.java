package com.ternaryop.photoshelf.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import org.junit.Test;

import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;

public class TitleParserTests {

    @Test
	public void testTitleParser() {
		BufferedReader titleReader = null;
		BufferedReader resultReader = null;
		
		try {
			titleReader = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/dave/Dropbox/devel/android/photoShelf/tests/titleParser-test-input.txt"), "UTF-8"));
			resultReader = new BufferedReader(new InputStreamReader(new FileInputStream("/Users/dave/Dropbox/devel/android/photoShelf/tests/titleParser-test-results.txt"), "UTF-8"));
			String line;
			int lineNumber = 1;
			int failCount = 0;
			String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
			
			while ((line = titleReader.readLine()) != null) {
				TitleData titleData = TitleParser.instance().parseTitle(line);
				String resultLine = resultReader.readLine().replace("%CURRENT_YEAR%", year);
				String str = titleData.format("<strong>", "</strong>", "<em>", "</em>");
				if (!resultLine.equals(str)) {
					System.out.println(lineNumber + ":\n" + line + "\n" + resultLine + "\n" + str);
					++failCount;
				}
				++lineNumber;
			}
			System.out.println("Fail count " + failCount);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (titleReader != null) try { titleReader.close(); } catch (IOException e) {}
			if (resultReader != null) try { resultReader.close(); } catch (IOException e) {}
		}
	}
}
