package com.ternaryop.photoshelf.dialogs.tagnavigator

class TagCounter(val tag: String) {
    var count: Int = 1

    override fun toString(): String {
        return if (count == 1) {
            tag
        } else "$tag ($count)"
    }

    fun compareTagTo(other: TagCounter): Int = tag.compareTo(other.tag, ignoreCase = true)

    companion object {
        fun fromStrings(tagList: List<String>): List<TagCounter> {
            val map = HashMap<String, TagCounter>(tagList.size)
            for (s in tagList) {
                val lower = s.toLowerCase()
                var tagCounter = map[lower]
                if (tagCounter == null) {
                    tagCounter = TagCounter(s)
                    map[lower] = tagCounter
                } else {
                    ++tagCounter.count
                }
            }
            return map.values.toList()
        }
    }
}