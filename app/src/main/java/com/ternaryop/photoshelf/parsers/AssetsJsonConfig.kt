package com.ternaryop.photoshelf.parsers

import android.content.Context
import com.ternaryop.utils.JSONUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Read config from json files stored on assets and check if upgraded files are present.
 * The json file must have an integer version field
 * Created by dave on 13/01/17.
 */

abstract class AssetsJsonConfig {
    abstract val version: Int

    @Throws(IOException::class, JSONException::class)
    protected fun readAssetsConfig(context: Context, fileName: String): JSONObject {
        val jsonAssets = jsonFromAssets(context, fileName)
        val jsonPrivate = jsonFromPrivateFile(context, fileName)
        if (jsonPrivate != null) {
            val assetsVersion = readVersion(jsonAssets)
            val privateVersion = readVersion(jsonPrivate)
            if (privateVersion > assetsVersion) {
                return jsonPrivate
            }
            context.deleteFile(fileName)
        }
        return jsonAssets
    }

    @Throws(IOException::class, JSONException::class)
    private fun jsonFromAssets(context: Context, fileName: String): JSONObject {
        context.assets.open(fileName).use { stream -> return JSONUtils.jsonFromInputStream(stream) }
    }

    @Throws(IOException::class, JSONException::class)
    private fun jsonFromPrivateFile(context: Context, fileName: String): JSONObject? {
        try {
            context.openFileInput(fileName).use { stream -> return JSONUtils.jsonFromInputStream(stream) }
        } catch (ex: FileNotFoundException) {
            return null
        }
    }

    protected fun readVersion(jsonObject: JSONObject): Int {
        // version may be not present on existing json file
        try {
            return jsonObject.getInt("version")
        } catch (ignored: JSONException) {
        }

        return -1
    }
}
