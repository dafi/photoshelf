package com.ternaryop.photoshelf.parsers

import java.util.Locale

class TitleData(private val config: TitleParserConfig) {
    var who = emptyList<String>()
        set(value) {
        field = ArrayList(value)
    }

    // remove all non alnum chars (except single and double quotes) from the end
    // lowercase the first character
    var location: String? = null
        set(value) {
            if (value == null) {
                field = null
                return
            }
            var location = value.replace("""[^\p{Alnum}"']*${'$'}""".toRegex(), "").trim()

            if (location.isEmpty()) {
                field = null
                return
            }
            location = if (hasLocationPrefix(location)) {
                location.substring(0, 1).toLowerCase(Locale.ENGLISH) + location.substring(1)
            } else {
                "at the $location"
            }
            field = location
        }
    var city: String? = null
        set(value) {
            field = value?.let { expandAbbreviation(it.trim()) }
        }
    var tags = emptyList<String>()
        set(value) {
            val list = mutableListOf<String>()
            for (tag1 in value) {
                val tag = tag1
                        .replaceFirst("[0-9]*(st|nd|rd|th)?".toRegex(), "")
                        .replace("""["']""".toRegex(), "")
                        .trim()
                if (tag.isNotEmpty()) {
                    list.add(tag)
                }
            }
            field = list
        }
    var eventDate: String? = null
        set(value) {
            field = value?.trim()
        }

    fun setWhoFromString(string: String) {
        who = """(?i)\s*(?:,|&|\band\b)\s*""".toRegex().split(string.trim())
    }

    private fun hasLocationPrefix(location: String): Boolean {
        return config.locationPrefixes.any { it.hasLocationPrefix(location) }
    }

    private fun expandAbbreviation(aCity: String): String? {
        if (aCity.isEmpty()) {
            return null
        }
        for ((key, value) in config.cities) {
            if (key.equals(aCity, ignoreCase = true) || value.matcher(aCity).find()) {
                return key
            }
        }
        return aCity
    }

    fun toHtml(): String {
        return format("<strong>", "</strong>", "<em>", "</em>")
    }

    fun format(whoTagOpen: String, whoTagClose: String, descTagOpen: String, descTagClose: String): String {
        val sb = formatWho(whoTagOpen, whoTagClose, descTagOpen, descTagClose, StringBuilder())
        if (this.location != null || this.eventDate != null || this.city != null) {
            if (sb.isNotEmpty()) {
                sb.append(" ")
            }
            sb.append(descTagOpen)
            if (this.location != null) {
                sb.append(this.location)
                if (this.city == null) {
                    sb.append(" ")
                } else {
                    sb.append(", ")
                }
            }
            if (this.city != null) {
                sb
                        .append(this.city)
                        .append(" ")
            }
            if (this.eventDate != null) {
                sb
                        .append("(")
                        .append(this.eventDate)
                        .append(")")
            }
            sb.append(descTagClose)
        }
        return sb.toString()
    }

    fun formatWho(whoTagOpen: String, whoTagClose: String, descTagOpen: String, descTagClose: String, sb: StringBuilder): StringBuilder {
        if (who.isEmpty()) {
            return sb
        }
        var appendSep = false
        for (i in 0 until who.size - 1) {
            if (appendSep) {
                sb.append(", ")
            } else {
                appendSep = true
            }
            sb.append(who[i])
        }
        if (who.size > 1) {
            sb.insert(0, whoTagOpen)
                    .append(whoTagClose)
                    .append(descTagOpen)
                    .append(" and ")
                    .append(descTagClose)
        }
        sb.append(whoTagOpen)
                .append(who[who.size - 1])
                .append(whoTagClose)
        return sb
    }
}
