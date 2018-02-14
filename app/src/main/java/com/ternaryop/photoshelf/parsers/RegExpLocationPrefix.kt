package com.ternaryop.photoshelf.parsers

import java.util.regex.Pattern

/**
 * Created by dave on 04/12/17.
 * Use regular expression to match location prefix
 */

class RegExpLocationPrefix(regExp: String) : LocationPrefix {
    private val pattern = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE)

    override fun hasLocationPrefix(location: String): Boolean {
        return pattern.matcher(location).find()
    }

    override fun removePrefix(target: String): String {
        return pattern.matcher(target).replaceFirst("")
    }
}
