package com.ternaryop.photoshelf.dialogs

import android.content.Context
import com.ternaryop.photoshelf.R
import com.ternaryop.photoshelf.customsearch.GoogleCustomSearchClient
import com.ternaryop.photoshelf.db.DBHelper

/**
 * Created by dave on 24/02/18.
 * Search for misspelled names
 */
class MisspelledName(val context: Context) {
    fun getMisspelledInfo(name: String): Pair<Int, String> {
        var pair = getMatchingName(name)
        if (pair != null) {
            return pair
        }
        pair = getMisspelledName(name)
        return if (pair != null) {
            pair
        } else Pair(NAME_NOT_FOUND, name)
    }

    private fun getMatchingName(name: String): Pair<Int, String>? {
        val correctedName = DBHelper.getInstance(context).tagMatcherDAO.getMatchingTag(name)
        if (name.equals(correctedName, ignoreCase = true)) {
            return Pair(NAME_ALREADY_EXISTS, name)
        }
        return if (correctedName == null) {
            null
        } else Pair(NAME_MISSPELLED, correctedName)
    }

    private fun getMisspelledName(name: String): Pair<Int, String>? {
        val correctedName = GoogleCustomSearchClient(
            context.getString(R.string.GOOGLE_CSE_APIKEY),
            context.getString(R.string.GOOGLE_CSE_CX))
            .getCorrectedQuery(name) ?: return null
        return Pair(NAME_MISSPELLED, correctedName)
    }

    companion object {
        const val NAME_ALREADY_EXISTS = 0
        const val NAME_NOT_FOUND = 1
        const val NAME_MISSPELLED = 2
    }
}