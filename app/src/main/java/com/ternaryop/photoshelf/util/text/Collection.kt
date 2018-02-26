package com.ternaryop.photoshelf.util.text

/**
 * Created by dave on 24/02/18.
 * Collection string extension
 */
fun List<String>.anyMatches(needle: String, ignoreCase: Boolean = true): Boolean {
    return any { s -> s.trim().compareTo(needle, ignoreCase) == 0 }
}
