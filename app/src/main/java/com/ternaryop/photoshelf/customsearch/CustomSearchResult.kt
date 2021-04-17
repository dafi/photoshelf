package com.ternaryop.photoshelf.customsearch

import com.squareup.moshi.JsonClass

/**
 * Created by dave on 01/05/17.
 * Hold the custom search result
 */

@JsonClass(generateAdapter = true)
data class CustomSearchResult(val spelling: Spelling?)

@JsonClass(generateAdapter = true)
data class Spelling(val correctedQuery: String?, val htmlCorrectedQuery: String?)
