package com.ternaryop.photoshelf.parsers

import java.util.regex.Pattern

/**
 * Created by dave on 21/03/15.
 * Access to TitleParser configuration
 */
interface TitleParserConfig {
    val titleCleanerList: List<TitleParserRegExp>
    val titleParserPattern: Pattern
    val locationPrefixes: List<LocationPrefix>
    val cities: Map<String, Pattern>
    fun applyList(titleParserRegExpList: List<TitleParserRegExp>, input: String): String

    class TitleParserRegExp(internal val pattern: String, internal val replacer: String) {
        companion object {

            fun applyList(titleParserRegExpList: List<TitleParserRegExp>, input: String): String {
                var result = input

                for (re in titleParserRegExpList) {
                    result = result.replace(re.pattern.toRegex(), re.replacer)
                }
                return result
            }
        }
    }
}
