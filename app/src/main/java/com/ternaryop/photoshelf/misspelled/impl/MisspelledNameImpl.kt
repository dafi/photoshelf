package com.ternaryop.photoshelf.misspelled.impl

import com.ternaryop.photoshelf.api.ApiManager
import com.ternaryop.photoshelf.customsearch.GoogleCustomSearchClient
import com.ternaryop.photoshelf.db.TagMatcherDAO
import com.ternaryop.photoshelf.misspelled.MisspelledName

/**
 * Created by dave on 24/02/18.
 * Search for misspelled names
 */
class MisspelledNameImpl(private val tagMatcherDAO: TagMatcherDAO) : MisspelledName {
    override suspend fun getMisspelledInfo(name: String): MisspelledName.Info {
        return getMatchingName(name)
            ?: getMisspelledName(name)
            ?: MisspelledName.Info.NotFound(name)
    }

    private suspend fun getMatchingName(name: String): MisspelledName.Info? {
        val correctedName = tagMatcherDAO.getMatchingTag(name)
            ?: getCorrectMisspelledName(name)
            ?: return null
        return if (name.equals(correctedName, ignoreCase = true)) {
            MisspelledName.Info.AlreadyExists(name)
        } else {
            MisspelledName.Info.Corrected(correctedName)
        }
    }

    private suspend fun getCorrectMisspelledName(name: String): String? {
        val corrected = ApiManager.postService().getCorrectMisspelledName(name).response.corrected ?: return null
        tagMatcherDAO.insert(corrected)
        return corrected
    }

    private suspend fun getMisspelledName(name: String): MisspelledName.Info? {
        val result = GoogleCustomSearchClient.getCorrectedQuery(name)

        val correctedQuery = result.spelling?.correctedQuery ?: return null
        return MisspelledName.Info.Corrected(correctedQuery)
    }
}
