package com.ternaryop.photoshelf.parsers

import android.content.Context
import java.util.regex.Pattern

/**
 * Created by dave on 21/03/15.
 * The Config to use under Android, read the config from assets and upgrade it if an imported version exists
 */
private const val TITLE_PARSER_FILENAME = "titleParser.json"

class AndroidTitleParserConfig(context: Context) : AssetsJsonConfig(), TitleParserConfig {

    override val titleCleanerList: List<TitleParserConfig.TitleParserRegExp>
        get() = titleParserConfig.titleCleanerList

    override val titleParserPattern: Pattern
        get() = titleParserConfig.titleParserPattern

    override val locationPrefixes: List<LocationPrefix>
        get() = titleParserConfig.locationPrefixes

    override val cities: Map<String, Pattern>
        get() = titleParserConfig.cities

    override val version: Int = _version

    init {
        if (version <= 0) {
            try {
                val jsonObject = readAssetsConfig(context, TITLE_PARSER_FILENAME)
                _version = readVersion(jsonObject)
                titleParserConfig.readConfig(jsonObject)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun applyList(titleParserRegExpList: List<TitleParserConfig.TitleParserRegExp>, input: String): String {
        return titleParserConfig.applyList(titleParserRegExpList, input)
    }

    companion object {
        private var _version = -1
        private val titleParserConfig = JSONTitleParserConfig()
    }
}
