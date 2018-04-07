package com.ternaryop.photoshelf.parsers

import com.ternaryop.utils.text.capitalizeAll
import com.ternaryop.utils.text.replaceUnicodeWithClosestAscii
import com.ternaryop.utils.text.stripAccents

class TitleParser private constructor(private val config: TitleParserConfig) {

    fun parseTitle(sourceTitle: String,
        swapDayMonth: Boolean = false, checkDateInTheFuture: Boolean = true): TitleData {
        var title = sourceTitle
        val titleData = TitleData(config)

        title = config.applyList(config.titleCleanerList, title)
            .trim()
            .replaceUnicodeWithClosestAscii()
            .stripAccents()

        title = setWho(title, titleData)

        val dateComponents = TitleDateComponents.extract(title, swapDayMonth, checkDateInTheFuture)
        setLocationAndCity(titleData, parseLocation(title, dateComponents))

        titleData.eventDate = dateComponents.format()
        titleData.tags = titleData.who

        return titleData
    }

    private fun setWho(sourceTitle: String, titleData: TitleData): String {
        var title = sourceTitle
        val m = config.titleParserPattern.matcher(title)
        if (m.find() && m.groupCount() > 1) {
            val who = m.group(1)
            titleData.setWhoFromString(who.capitalizeAll())
            // remove the 'who' chunk and any not alphabetic character
            // (eg the dash used to separated "who" from location)
            title = if (Character.isLetter(m.group(2)[0])) {
                title.substring(who.length)
            } else {
                title.substring(m.group(0).length)
            }
        }
        return title
    }

    private fun parseLocation(title: String, dateComponents: DateComponents): String {
        return if (dateComponents.datePosition < 0) {
            // no date found so use all substring as location
            title
        } else title.substring(0, dateComponents.datePosition)
    }

    private fun setLocationAndCity(titleData: TitleData, loc: String) {
        // city names can be multi words so allow whitespaces
        val m = """\s?(.*)?\s?\bin\b([a-z.\s']*).*${'$'}""".toRegex(RegexOption.IGNORE_CASE).matchEntire(loc)
        if (m == null) {
            titleData.location = loc
        } else {
            titleData.location = m.groupValues[1]
            titleData.city = m.groupValues[2].trim { it <= ' ' }
        }
    }

    companion object {
        private var instance: TitleParser? = null

        fun instance(config: TitleParserConfig): TitleParser {
            if (instance == null) {
                synchronized(TitleParser::class.java) {
                    if (instance == null) {
                        instance = TitleParser(config)
                    }
                }
            }
            return instance!!
        }
    }
}
