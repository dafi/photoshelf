package com.ternaryop.photoshelf.domselector

import android.content.Context
import android.os.Environment
import com.google.gson.GsonBuilder
import com.ternaryop.utils.log.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Obtain the DOM selector used to extract gallery and images contained inside a given url
 *
 * @author dave
 */
object DomSelectorManager {
    private var domSelectors: DomSelectors? = null
    private const val SELECTORS_FILENAME = "domSelectors.json"
    private const val LOG_FILENAME = "photoshelf.txt"

    fun selectors(context: Context): DomSelectors {
        synchronized(DomSelectors::class.java) {
            if (upgradeConfig(context)) {
                domSelectors = null
            }
            if (domSelectors == null) {
                try {
                    domSelectors = openConfig(context).use { stream ->
                        GsonBuilder().create().fromJson(InputStreamReader(stream), DomSelectors::class.java)
                    }
                } catch (e: Exception) {
                    domSelectors = DomSelectors(-1, emptyList())
                    log(e)
                }
            }
        }
        return domSelectors!!
    }

    private fun openConfig(context: Context): InputStream {
        return try {
            context.openFileInput(SELECTORS_FILENAME)
        } catch (ex: FileNotFoundException) {
            copyConfig(context, context.assets.open(SELECTORS_FILENAME))
            context.openFileInput(SELECTORS_FILENAME)
        }
    }

    private fun copyConfig(context: Context, input: InputStream) {
        input.use { stream -> context.openFileOutput(SELECTORS_FILENAME, 0).use { out -> stream.copyTo(out) } }
    }

    private fun upgradeConfig(context: Context): Boolean {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SELECTORS_FILENAME)
        if (!file.exists()) {
            return false
        }
        try {
            copyConfig(context, FileInputStream(file.absolutePath))
            file.delete()
            return true
        } catch (e: Exception) {
            log(e)
        }
        return false
    }

    private fun log(e: Throwable) {
        Log.error(e, File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOG_FILENAME))
    }
}
