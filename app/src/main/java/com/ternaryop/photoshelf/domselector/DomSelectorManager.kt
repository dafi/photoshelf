package com.ternaryop.photoshelf.domselector

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.google.gson.GsonBuilder
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

    fun selectors(context: Context): DomSelectors {
        synchronized(DomSelectors::class.java) {
            if (domSelectors == null) {
                domSelectors = try {
                    openConfig(context).use { stream ->
                        GsonBuilder().create().fromJson(InputStreamReader(stream), DomSelectors::class.java)
                    }
                } catch (e: Exception) {
                    DomSelectors(-1, emptyList())
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

    fun upgradeConfig(context: Context, uri: Uri) {
        checkNotNull(context.contentResolver.openInputStream(uri)) { "Unable to read configuration" }.also { stream ->
            copyConfig(context, stream)
            DocumentsContract.deleteDocument(context.contentResolver, uri)
            domSelectors = null
        }
    }
}
