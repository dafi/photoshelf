package com.ternaryop.photoshelf.customsearch

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by dave on 01/05/17.
 * Hold the custom search result
 */

@Suppress("MemberVisibilityCanBePrivate")
class CustomSearchResult @Throws(JSONException::class) constructor(json: JSONObject) {
    var correctedQuery: String? = null

    companion object {

        @Throws(JSONException::class)
        fun getCorrectedQuery(json: JSONObject): String? {
            return if (!json.has("spelling")) {
                null
            } else json.getJSONObject("spelling").getString("correctedQuery")
        }
    }

    init {
        correctedQuery = getCorrectedQuery(json)
    }
}
