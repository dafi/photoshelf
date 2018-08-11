package com.ternaryop.photoshelf.customsearch

/**
 * Created by dave on 01/05/17.
 * Hold the custom search result
 */

data class CustomSearchResult(val spelling: Spelling?)
data class Spelling(val correctedQuery: String?, val htmlCorrectedQuery: String?)
