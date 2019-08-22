package com.ternaryop.photoshelf.dialogs

import android.content.Context
import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.customsearch.GoogleCustomSearchClient
import com.ternaryop.photoshelf.db.DBHelper

/**
 * Created by dave on 24/02/18.
 * Search for misspelled names
 */
class MisspelledName(val context: Context) {
    suspend fun getMisspelledInfo(name: String): Pair<Int, String> {
        return getMatchingName(name)
            ?: getMisspelledName(name)
            ?: Pair(NAME_NOT_FOUND, name)
    }

    private suspend fun getMatchingName(name: String): Pair<Int, String>? {
        val correctedName = DBHelper.getInstance(context).tagMatcherDAO.getMatchingTag(name)
            ?: getCorrectMisspelledName(name)
            ?: return null
        return if (name.equals(correctedName, ignoreCase = true)) {
            Pair(NAME_ALREADY_EXISTS, name)
        } else {
            Pair(NAME_MISSPELLED, correctedName)
        }
    }

    private suspend fun getCorrectMisspelledName(name: String): String? {
        val corrected = ApiManager.postService().getCorrectMisspelledName(name).response.corrected ?: return null
        DBHelper.getInstance(context).tagMatcherDAO.insert(corrected)
        return corrected
    }

    private suspend fun getMisspelledName(name: String): Pair<Int, String>? {
        val result = GoogleCustomSearchClient.getCorrectedQuery(name)

        val correctedQuery = result.spelling?.correctedQuery ?: return null
        return Pair(NAME_MISSPELLED, correctedQuery)
    }

    companion object {
        const val NAME_ALREADY_EXISTS = 0
        const val NAME_NOT_FOUND = 1
        const val NAME_MISSPELLED = 2
    }
}