package com.ternaryop.photoshelf.parsers

import com.ternaryop.utils.json.readJson
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream
import java.util.regex.Pattern

/**
 * Created by dave on 09/01/16.
 * Read from json file
 */
class JSONTitleParserConfig : TitleParserConfig {
    override lateinit var titleCleanerList: List<TitleParserConfig.TitleParserRegExp>
        private set
    override lateinit var titleParserPattern: Pattern
        private set
    override lateinit var locationPrefixes: List<LocationPrefix>
        private set
    override lateinit var cities: Map<String, Pattern>
        private set

    constructor()

    @Throws(Exception::class) constructor(jsonPath: String) {
        readConfig(jsonPath)
    }

    @Throws(Exception::class)
    private fun readConfig(jsonPath: String) {
        FileInputStream(jsonPath).use { stream -> readConfig(stream.readJson()) }
    }

    @Throws(Exception::class)
    fun readConfig(jsonObject: JSONObject) {
        titleCleanerList = createList(jsonObject, "titleCleaner", "regExprs")
        titleParserPattern = Pattern.compile(jsonObject.getString("titleParserRegExp"), Pattern.CASE_INSENSITIVE)
        locationPrefixes = readLocationPrefixes(jsonObject)
        cities = readCities(jsonObject)
    }

    @Throws(JSONException::class)
    private fun readCities(jsonObject: JSONObject): Map<String, Pattern> {
        val map: HashMap<String, Pattern> = HashMap()
        val jsonCities = jsonObject.getJSONObject("cities")
        val keys = jsonCities.keys()

        while (keys.hasNext()) {
            val k = keys.next() as String
            map[k] = Pattern.compile(jsonCities.getString(k))
        }
        return map
    }

    @Throws(JSONException::class)
    private fun readLocationPrefixes(jsonObject: JSONObject): List<LocationPrefix> {
        val jsonArray = jsonObject.getJSONArray("locationPrefixes")

        return (0 until jsonArray.length()).map { RegExpLocationPrefix(jsonArray.getString(it)) }
    }

    override fun applyList(titleParserRegExpList: List<TitleParserConfig.TitleParserRegExp>, input: String): String {
        return TitleParserConfig.TitleParserRegExp.applyList(titleParserRegExpList, input)
    }

    companion object {

        @Throws(Exception::class)
        fun createList(jsonAssets: JSONObject, rootName: String, replacers: String):
            MutableList<TitleParserConfig.TitleParserRegExp> {
            val list = mutableListOf<TitleParserConfig.TitleParserRegExp>()
            val array = jsonAssets.getJSONObject(rootName).getJSONArray(replacers)
            for (i in 0 until array.length()) {
                val reArray = array.getJSONArray(i)
                list.add(TitleParserConfig.TitleParserRegExp(reArray.getString(0), reArray.getString(1)))
            }
            return list
        }
    }
}
