package com.ternaryop.photoshelf.parsers

/**
 * Created by dave on 04/12/17.
 * The Location Prefix matcher
 */

interface LocationPrefix {
    fun hasLocationPrefix(location: String): Boolean
    fun removePrefix(target: String): String
}
