package com.ternaryop.utils.reactivex;

import java.io.File;
import java.io.IOException;

import android.os.Environment;
import android.support.annotation.NonNull;

import com.ternaryop.photoshelf.util.log.Log;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.functions.Consumer;

/**
 * Created by dave on 19/10/17.
 * Protect against app crashes when undeliverable errors occur
 */

public class UndeliverableErrorHandler implements Consumer<Throwable> {
    public static final String LOG_FILE_NAME = "rx_errors.txt";

    @Override
    public void accept(Throwable t) throws Exception {
        Log.error(t, getLogPath(), "Catched error from RX");
        if (t instanceof UndeliverableException) {
            t = t.getCause();
        }
        if ((t instanceof IOException)) {
            // fine, irrelevant network problem or API that throws on cancellation
            return;
        }
        if (t instanceof InterruptedException) {
            // fine, some blocking code was interrupted by a dispose call
            return;
        }
        if ((t instanceof NullPointerException) || (t instanceof IllegalArgumentException)) {
            // that's likely a bug in the application
            Thread.currentThread().getUncaughtExceptionHandler()
                    .uncaughtException(Thread.currentThread(), t);
            return;
        }
        if (t instanceof IllegalStateException) {
            // that's a bug in RxJava or in a custom operator
            Thread.currentThread().getUncaughtExceptionHandler()
                    .uncaughtException(Thread.currentThread(), t);
        }
    }

    @NonNull
    private File getLogPath() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOG_FILE_NAME);
    }
}
