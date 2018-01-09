package com.ternaryop.photoshelf.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Locale;

import com.ternaryop.photoshelf.parsers.JSONTitleParserConfig;
import com.ternaryop.photoshelf.parsers.TitleData;
import com.ternaryop.photoshelf.parsers.TitleParser;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Created by dave on 08/01/18.
 * This isn't a test but help to save to file the formatted output
 */
public class TitleSaveFormattedTest {
    @Test
    public void saveFormattedTitles() {
        final String titlesDirectoryPath = "/opt/devel/0dafiprj/git.github/post_title_downloader/titles/";
        final String formattedPath = "/opt/devel/0dafiprj/git.github/post_title_downloader/titles/all-formatted.txt";

        final File[] titleList = new File(titlesDirectoryPath).listFiles(file -> file.getName().toLowerCase(Locale.US).endsWith(".txt"));
        final File formattedFile = new File(formattedPath);

        if (!containsNewerThan(titleList, formattedFile)) {
            return;
        }
        Arrays.sort(titleList);

        try (PrintWriter pw = new PrintWriter(new FileWriter(formattedPath))) {
            String configPath = new File(new File(".").getAbsoluteFile(), "app/src/main/assets/titleParser.json").getAbsolutePath();
            final TitleParser instance = TitleParser.instance(new JSONTitleParserConfig(configPath));

            for (File f: titleList) {
                appendFormatted(f, pw, instance);
            }
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
            assertFalse(e.getMessage(), true);
        }
    }

    private boolean containsNewerThan(File[] files, File newerFile) {
        if (!newerFile.exists()) {
            return true;
        }
        final long newerLastModified = newerFile.lastModified();
        for (File f : files) {
            if (f.lastModified() > newerLastModified) {
                return true;
            }
        }
        return false;
    }

    private void appendFormatted(File file, PrintWriter appender, TitleParser titleParser) throws Exception {
        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String inputLine;

            final StringBuilder sb = new StringBuilder();
            while ((inputLine = inputReader.readLine()) != null) {
                TitleData titleData = titleParser.parseTitle(inputLine, false, false);
                sb.setLength(0);
                titleData.formatWho("", "", "", "", sb);
                appender.println(sb);
            }
        }
    }
}
