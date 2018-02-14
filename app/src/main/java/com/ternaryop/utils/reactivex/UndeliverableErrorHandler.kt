package com.ternaryop.utils.reactivex

import android.os.Environment
import com.ternaryop.photoshelf.util.log.Log
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.functions.Consumer
import java.io.File
import java.io.IOException

/**
 * Created by dave on 19/10/17.
 * Protect against app crashes when undeliverable errors occur
 */

class UndeliverableErrorHandler : Consumer<Throwable> {

    private val logPath: File
        get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOG_FILE_NAME)

    @Throws(Exception::class)
    override fun accept(throwable: Throwable) {
        var t = throwable
        Log.error(t, logPath, "Catched error from RX")
        if (t is UndeliverableException) {
            t = t.cause!!
        }
        if (t is IOException) {
            // fine, irrelevant network problem or API that throws on cancellation
            return
        }
        if (t is InterruptedException) {
            // fine, some blocking code was interrupted by a dispose call
            return
        }
        if (t is NullPointerException || t is IllegalArgumentException) {
            // that's likely a bug in the application
            Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t)
            return
        }
        if (t is IllegalStateException) {
            // that's a bug in RxJava or in a custom operator
            Thread.currentThread().uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), t)
        }
    }

    companion object {
        const val LOG_FILE_NAME = "rx_errors.txt"
    }
}
