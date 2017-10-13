package com.ternaryop.photoshelf.util.log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by dave on 23/06/16.
 * helper class to log errors to file
 */
public class Log {
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

    public static void error(Throwable t, File destFile, String... msg) {
        String date = DATE_TIME_FORMAT.format(new Date());
        try (FileOutputStream fos = new FileOutputStream(destFile, true)) {
            PrintStream ps = new PrintStream(fos);
            if (msg != null) {
                for (String m : msg) {
                    ps.println(date + " - " + m);
                }
            }
            t.printStackTrace(ps);
            ps.flush();
            ps.close();
        } catch (Exception fosEx) {
            fosEx.printStackTrace();
        }
    }

}
