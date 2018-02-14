package com.ternaryop.photoshelf.util.sort

/**
 * Created by dave on 12/06/16.
 * Sort items and toggle the direction
 */
interface Sortable {
    val isDefaultAscending: Boolean
    var isAscending: Boolean
    fun sort()
}
